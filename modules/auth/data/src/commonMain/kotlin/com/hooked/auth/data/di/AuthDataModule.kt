package com.hooked.auth.data.di

import com.hooked.auth.data.api.AuthApiService
import com.hooked.auth.data.datasources.AuthDataSource
import com.hooked.auth.data.datasources.RemoteAuthDataSource
import com.hooked.auth.data.datasources.StubAuthDataSource
import com.hooked.auth.data.repositories.AuthRepositoryImpl
import com.hooked.auth.data.storage.TokenStorage
import com.hooked.auth.domain.repositories.AuthRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authDataModule = module {
    // Storage - platform-specific implementation will be provided by platform modules
    
    // API Service
    single<AuthApiService> { AuthApiService(get()) }
    
    // Data Sources
    single<AuthDataSource>(named("stub")) { StubAuthDataSource() }
    single<AuthDataSource>(named("remote")) { RemoteAuthDataSource(get(), get()) }
    
    // Use remote by default, can be switched to stub for testing
    single<AuthDataSource> { get<AuthDataSource>(named("remote")) }
    
    // Repository
    single<AuthRepository> { AuthRepositoryImpl(get()) }
}