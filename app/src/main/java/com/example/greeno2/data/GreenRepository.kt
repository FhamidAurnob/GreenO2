package com.example.greeno2.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.example.greeno2.domain.CarbonCalculator
import com.example.greeno2.domain.PasswordHasher
import com.example.greeno2.domain.TripValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId

class GreenRepository(private val database: GreenDatabase) {

    /** Bumped after every write so screens know to reload. */
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes.asStateFlow()

    private fun notifyChanged() = _changes.update { it + 1 }

    // ---------- Auth ----------

    suspend fun register(name: String, email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            val hashed = PasswordHasher.hash(password)
            val now = System.currentTimeMillis()
            try {
                val id = database.writableDatabase.insertOrThrow("users", null, ContentValues().apply {
                    put("name", name.trim())
                    put("email", email.trim().lowercase())
                    put("password_hash", hashed.hash)
                    put("password_salt", hashed.salt)
                    put("is_demo", 0)
                    put("created_at", now)
                })
                notifyChanged()
                Result.success(User(id, name.trim(), email.trim().lowercase(), now))
            } catch (e: SQLiteConstraintException) {
                Result.failure(IllegalArgumentException("An account with this email already exists"))
            }
        }

    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT * FROM users WHERE email = ? AND is_demo = 0",
            arrayOf(email.trim().lowercase()),
        ).use { c ->
            if (!c.moveToFirst()) return@withContext Result.failure(IllegalArgumentException("Incorrect email or password"))
            val ok = PasswordHasher.verify(password, c.str("password_hash"), c.str("password_salt"))
            if (!ok) return@withContext Result.failure(IllegalArgumentException("Incorrect email or password"))
            Result.success(c.toUser())
        }
    }

    suspend fun getUser(id: Long): User? = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT * FROM users WHERE id = ?",
            arrayOf(id.toString()),
        ).use { c -> if (c.moveToFirst()) c.toUser() else null }
    }

    // ---------- Onboarding quiz ----------

    /** Saves quiz results and awards the one-time welcome bonus. Returns the points awarded. */
    suspend fun completeQuiz(userId: Long, score: Int, preferredMode: TransportMode?, bonus: Int): Int =
        withContext(Dispatchers.IO) {
            database.writableDatabase.update(
                "users",
                ContentValues().apply {
                    put("quiz_state", QuizState.COMPLETED.ordinal)
                    put("eco_score", score)
                    if (preferredMode != null) put("preferred_mode", preferredMode.name) else putNull("preferred_mode")
                },
                "id = ?", arrayOf(userId.toString()),
            )
            val awarded = if (addBonus(userId, "quiz", "welcome", bonus)) bonus else 0
            notifyChanged()
            awarded
        }

    suspend fun skipQuiz(userId: Long) = withContext(Dispatchers.IO) {
        database.writableDatabase.update(
            "users", ContentValues().apply { put("quiz_state", QuizState.SKIPPED.ordinal) },
            "id = ? AND quiz_state = ?", arrayOf(userId.toString(), QuizState.NOT_SEEN.ordinal.toString()),
        )
        notifyChanged()
    }

    // ---------- Challenges ----------

    suspend fun joinChallenge(userId: Long, challengeId: String, period: String) = withContext(Dispatchers.IO) {
        database.writableDatabase.insertWithOnConflict(
            "challenge_joins", null,
            ContentValues().apply {
                put("user_id", userId)
                put("challenge_id", challengeId)
                put("period", period)
                put("joined_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        notifyChanged()
    }

    suspend fun joinedChallenges(userId: Long, period: String): Set<String> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT challenge_id FROM challenge_joins WHERE user_id = ? AND period = ?",
            arrayOf(userId.toString(), period),
        ).use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
    }

    suspend fun claimedChallenges(userId: Long, period: String): Set<String> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT ref FROM bonuses WHERE user_id = ? AND kind = 'challenge' AND ref LIKE ?",
            arrayOf(userId.toString(), "%@$period"),
        ).use { c -> buildSet { while (c.moveToNext()) add(c.getString(0).substringBefore('@')) } }
    }

    /** Awards a completed challenge's points once per week. Returns false if already claimed. */
    suspend fun claimChallenge(userId: Long, challengeId: String, period: String, points: Int): Boolean =
        withContext(Dispatchers.IO) {
            val ok = addBonus(userId, "challenge", "$challengeId@$period", points)
            notifyChanged()
            ok
        }

    suspend fun updateName(userId: Long, name: String) = withContext(Dispatchers.IO) {
        database.writableDatabase.update(
            "users", ContentValues().apply { put("name", name.trim()) },
            "id = ?", arrayOf(userId.toString()),
        )
        notifyChanged()
    }

    // ---------- Trips ----------

    /**
     * Scores, validates and stores a finished trip. Runs the anti-cheat rules and the daily
     * credit cap, so the stored trip is the source of truth for credits.
     */
    suspend fun saveTrip(
        userId: Long,
        mode: TransportMode,
        startTime: Long,
        endTime: Long,
        distanceMeters: Double,
        maxSegmentSpeedKmh: Double,
    ): Trip = withContext(Dispatchers.IO) {
        val calc = CarbonCalculator.calculate(mode, distanceMeters)
        val verdict = TripValidator.validate(mode, distanceMeters, endTime - startTime, maxSegmentSpeedKmh)

        val savedKg = if (verdict.eligible) calc.savedKg else 0.0
        var credits = if (verdict.eligible) calc.credits else 0
        var note = verdict.reason

        if (credits > 0) {
            val earnedToday = creditsEarnedSince(userId, startOfToday())
            val remaining = (DAILY_CREDIT_CAP - earnedToday).coerceAtLeast(0)
            if (credits > remaining) {
                credits = remaining
                note = "Daily limit of $DAILY_CREDIT_CAP Green Points reached"
            }
        }

        val id = database.writableDatabase.insert("trips", null, ContentValues().apply {
            put("user_id", userId)
            put("mode", mode.name)
            put("start_time", startTime)
            put("end_time", endTime)
            put("distance_m", distanceMeters)
            put("emitted_kg", calc.emittedKg)
            put("saved_kg", savedKg)
            put("credits", credits)
            put("flagged", if (verdict.suspicious) 1 else 0)
            if (note != null) put("note", note) else putNull("note")
        })
        notifyChanged()
        Trip(id, userId, mode, startTime, endTime, distanceMeters, calc.emittedKg, savedKg, credits, verdict.suspicious, note)
    }

    suspend fun trips(userId: Long): List<Trip> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT * FROM trips WHERE user_id = ? ORDER BY start_time DESC",
            arrayOf(userId.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Trip(
                            id = c.long("id"),
                            userId = c.long("user_id"),
                            mode = TransportMode.fromName(c.str("mode")),
                            startTime = c.long("start_time"),
                            endTime = c.long("end_time"),
                            distanceMeters = c.dbl("distance_m"),
                            emittedKg = c.dbl("emitted_kg"),
                            savedKg = c.dbl("saved_kg"),
                            credits = c.int("credits"),
                            flagged = c.int("flagged") == 1,
                            note = c.strOrNull("note"),
                        )
                    )
                }
            }
        }
    }

    suspend fun stats(userId: Long): UserStats = withContext(Dispatchers.IO) {
        val db = database.readableDatabase
        val base = db.rawQuery(
            """
            SELECT COUNT(id) AS n, COALESCE(SUM(distance_m), 0) AS dist,
                   COALESCE(SUM(emitted_kg), 0) AS emitted, COALESCE(SUM(saved_kg), 0) AS saved,
                   COALESCE(SUM(credits), 0) AS credits
            FROM trips WHERE user_id = ?
            """.trimIndent(),
            arrayOf(userId.toString()),
        ).use { c ->
            c.moveToFirst()
            UserStats(
                tripCount = c.int("n"),
                distanceKm = c.dbl("dist") / 1000.0,
                emittedKg = c.dbl("emitted"),
                savedKg = c.dbl("saved"),
                creditsEarned = c.int("credits"),
            )
        }
        val bonus = bonusPoints(userId)
        base.copy(
            creditsEarned = base.creditsEarned + bonus,
            creditsSpent = creditsSpent(userId),
            bonusPoints = bonus,
        )
    }

    // ---------- Community ----------

    suspend fun leaderboard(limit: Int = 50): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            """
            SELECT u.id AS id, u.name AS name, u.is_demo AS is_demo,
                   COALESCE(SUM(t.saved_kg), 0) AS saved,
                   COALESCE(SUM(t.distance_m), 0) AS dist,
                   COUNT(t.id) AS n
            FROM users u
            LEFT JOIN trips t ON t.user_id = u.id AND t.flagged = 0
            GROUP BY u.id
            ORDER BY saved DESC, dist DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        LeaderboardEntry(
                            userId = c.long("id"),
                            name = c.str("name"),
                            isDemo = c.int("is_demo") == 1,
                            savedKg = c.dbl("saved"),
                            distanceKm = c.dbl("dist") / 1000.0,
                            tripCount = c.int("n"),
                        )
                    )
                }
            }
        }
    }

    // ---------- Mart ----------

    suspend fun redeem(userId: Long, item: MartItem): Result<Redemption> = withContext(Dispatchers.IO) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val earned = creditsEarnedSince(userId, 0L) + bonusPoints(userId)
            val balance = earned - creditsSpent(userId)
            if (balance < item.cost) {
                return@withContext Result.failure(
                    IllegalStateException("You need ${item.cost - balance} more Green Points for this reward")
                )
            }
            val code = voucherCode()
            val now = System.currentTimeMillis()
            val id = db.insert("redemptions", null, ContentValues().apply {
                put("user_id", userId)
                put("item_id", item.id)
                put("title", item.title)
                put("cost", item.cost)
                put("code", code)
                put("time", now)
            })
            db.setTransactionSuccessful()
            Result.success(Redemption(id, item.id, item.title, item.cost, code, now))
        } finally {
            db.endTransaction()
            notifyChanged()
        }
    }

    suspend fun redemptions(userId: Long): List<Redemption> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT * FROM redemptions WHERE user_id = ? ORDER BY time DESC",
            arrayOf(userId.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Redemption(
                            id = c.long("id"),
                            itemId = c.str("item_id"),
                            title = c.str("title"),
                            cost = c.int("cost"),
                            code = c.str("code"),
                            time = c.long("time"),
                        )
                    )
                }
            }
        }
    }

    // ---------- helpers ----------

    private fun creditsEarnedSince(userId: Long, since: Long): Int =
        database.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(credits), 0) FROM trips WHERE user_id = ? AND start_time >= ?",
            arrayOf(userId.toString(), since.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun bonusPoints(userId: Long): Int =
        database.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(points), 0) FROM bonuses WHERE user_id = ?",
            arrayOf(userId.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    /** Inserts a bonus unless the same (kind, ref) was already awarded. Returns true if added. */
    private fun addBonus(userId: Long, kind: String, ref: String, points: Int): Boolean {
        if (points <= 0) return false
        val id = database.writableDatabase.insertWithOnConflict(
            "bonuses", null,
            ContentValues().apply {
                put("user_id", userId)
                put("kind", kind)
                put("ref", ref)
                put("points", points)
                put("time", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        return id != -1L
    }

    private fun creditsSpent(userId: Long): Int =
        database.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(cost), 0) FROM redemptions WHERE user_id = ?",
            arrayOf(userId.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun voucherCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        val part = { (1..4).map { chars[rnd.nextInt(chars.length)] }.joinToString("") }
        return "GO2-${part()}-${part()}"
    }

    companion object {
        /** Caps how many Green Points one account can earn from trips per calendar day. */
        const val DAILY_CREDIT_CAP = 300
    }
}

private fun Cursor.toUser() = User(
    id = long("id"),
    name = str("name"),
    email = str("email"),
    createdAt = long("created_at"),
    quizState = QuizState.of(int("quiz_state")),
    ecoScore = int("eco_score"),
    preferredMode = strOrNull("preferred_mode")?.let { TransportMode.fromName(it) },
)

private fun Cursor.long(col: String) = getLong(getColumnIndexOrThrow(col))
private fun Cursor.int(col: String) = getInt(getColumnIndexOrThrow(col))
private fun Cursor.dbl(col: String) = getDouble(getColumnIndexOrThrow(col))
private fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col))
private fun Cursor.strOrNull(col: String): String? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getString(i)
}
