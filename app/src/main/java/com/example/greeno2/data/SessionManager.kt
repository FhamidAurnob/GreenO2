package com.example.greeno2.data

import android.content.Context

/** Remembers who is logged in between app launches. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("green_miles_session", Context.MODE_PRIVATE)

    var userId: Long?
        get() = prefs.getLong(KEY_USER_ID, -1L).takeIf { it > 0 }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_USER_ID) else putLong(KEY_USER_ID, value)
            }.apply()
        }

    private companion object {
        const val KEY_USER_ID = "user_id"
    }
}
