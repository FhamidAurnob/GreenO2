package com.example.greeno2.data

import android.content.Context

/** Remembers who is logged in, plus small per-device UI flags. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("green_miles_session", Context.MODE_PRIVATE)

    var userId: Long?
        get() = prefs.getLong(KEY_USER_ID, -1L).takeIf { it > 0 }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_USER_ID) else putLong(KEY_USER_ID, value)
            }.apply()
        }

    /** Whether this user has dismissed the "How Rewards Work" guide. */
    fun rewardsGuideSeen(userId: Long): Boolean = prefs.getBoolean("$KEY_GUIDE$userId", false)

    fun setRewardsGuideSeen(userId: Long) {
        prefs.edit().putBoolean("$KEY_GUIDE$userId", true).apply()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_GUIDE = "rewards_guide_seen_"
    }
}
