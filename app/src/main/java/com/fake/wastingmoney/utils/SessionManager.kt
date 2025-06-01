package com.fake.wastingmoney.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("BudgetTrackerSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun createLoginSession(userId: Long, username: String) {
        val editor = sharedPreferences.edit()
        editor.putLong(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getUserId(): Long = sharedPreferences.getLong(KEY_USER_ID, -1)
    fun getUsername(): String = sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}