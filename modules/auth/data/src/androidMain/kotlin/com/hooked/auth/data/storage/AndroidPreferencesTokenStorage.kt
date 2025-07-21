package com.hooked.auth.data.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple SharedPreferences-based token storage.
 * Less secure than AndroidSecureTokenStorage but simpler and faster.
 * Use this for development or when security requirements are lower.
 */
class AndroidPreferencesTokenStorage(context: Context) : TokenStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "hooked_auth_prefs", 
        Context.MODE_PRIVATE
    )
    
    override suspend fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    override suspend fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    override suspend fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
    
    override suspend fun saveUser(user: String) {
        prefs.edit().putString(KEY_USER, user).apply()
    }
    
    override suspend fun getUser(): String? {
        return prefs.getString(KEY_USER, null)
    }
    
    override suspend fun clearUser() {
        prefs.edit().remove(KEY_USER).apply()
    }
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
    }
}