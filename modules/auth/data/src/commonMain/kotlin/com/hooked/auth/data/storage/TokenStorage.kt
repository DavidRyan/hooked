package com.hooked.auth.data.storage

interface TokenStorage {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
    suspend fun saveUser(user: String)
    suspend fun getUser(): String?
    suspend fun clearUser()
}