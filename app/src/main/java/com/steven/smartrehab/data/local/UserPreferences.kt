package com.steven.smartrehab.data.local

import android.content.Context

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("smartrehab_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_NAME, value.trim()).apply()
        }

    /** "left" ou "right" */
    var defaultSide: String
        get() = prefs.getString(KEY_SIDE, "left") ?: "left"
        set(value) {
            prefs.edit().putString(KEY_SIDE, value).apply()
        }

    private companion object {
        const val KEY_NAME = "user_name"
        const val KEY_SIDE = "default_side"
    }
}