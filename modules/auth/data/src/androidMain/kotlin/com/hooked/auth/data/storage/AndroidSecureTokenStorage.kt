package com.hooked.auth.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidSecureTokenStorage(context: Context) : TokenStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "hooked_secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
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