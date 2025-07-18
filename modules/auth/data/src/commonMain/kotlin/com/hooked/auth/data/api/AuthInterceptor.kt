package com.hooked.auth.data.api

import com.hooked.auth.data.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

class AuthInterceptor(private val tokenStorage: TokenStorage) {
    
    class Config {
        var tokenStorage: TokenStorage? = null
    }
    
    companion object : HttpClientPlugin<Config, AuthInterceptor> {
        override val key = AttributeKey<AuthInterceptor>("AuthInterceptor")
        
        override fun prepare(block: Config.() -> Unit): AuthInterceptor {
            val config = Config().apply(block)
            return AuthInterceptor(config.tokenStorage!!)
        }
        
        override fun install(plugin: AuthInterceptor, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val token = plugin.tokenStorage.getToken()
                val path = context.url.pathSegments.joinToString("/")
                if (token != null && !path.contains("auth/login") && !path.contains("auth/register")) {
                    context.header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}