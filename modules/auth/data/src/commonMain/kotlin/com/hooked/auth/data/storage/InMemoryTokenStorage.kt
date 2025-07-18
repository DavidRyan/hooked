package com.hooked.auth.data.storage

class InMemoryTokenStorage : TokenStorage {
    private var token: String? = null
    private var user: String? = null
    
    override suspend fun saveToken(token: String) {
        this.token = token
    }
    
    override suspend fun getToken(): String? {
        return token
    }
    
    override suspend fun clearToken() {
        token = null
    }
    
    override suspend fun saveUser(user: String) {
        this.user = user
    }
    
    override suspend fun getUser(): String? {
        return user
    }
    
    override suspend fun clearUser() {
        user = null
    }
}