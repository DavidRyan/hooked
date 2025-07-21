package com.hooked.auth.data.storage

import platform.Foundation.NSUserDefaults

class IosSecureTokenStorage : TokenStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    override suspend fun saveToken(token: String) {
        userDefaults.setObject(token, KEY_TOKEN)
        userDefaults.synchronize()
    }
    
    override suspend fun getToken(): String? {
        return userDefaults.stringForKey(KEY_TOKEN)
    }
    
    override suspend fun clearToken() {
        userDefaults.removeObjectForKey(KEY_TOKEN)
        userDefaults.synchronize()
    }
    
    override suspend fun saveUser(user: String) {
        userDefaults.setObject(user, KEY_USER)
        userDefaults.synchronize()
    }
    
    override suspend fun getUser(): String? {
        return userDefaults.stringForKey(KEY_USER)
    }
    
    override suspend fun clearUser() {
        userDefaults.removeObjectForKey(KEY_USER)
        userDefaults.synchronize()
    }
    
    companion object {
        private const val KEY_TOKEN = "hooked_auth_token"
        private const val KEY_USER = "hooked_user_data"
    }
}