package com.hooked.auth.domain.di

import com.hooked.auth.domain.usecases.GetCurrentUserUseCase
import com.hooked.auth.domain.usecases.LoginUseCase
import com.hooked.auth.domain.usecases.LogoutUseCase
import com.hooked.auth.domain.usecases.RefreshTokenUseCase
import com.hooked.auth.domain.usecases.RegisterUseCase
import org.koin.dsl.module

val authUseCaseModule = module {
    single { LoginUseCase(get()) }
    single { RegisterUseCase(get()) }
    single { LogoutUseCase(get()) }
    single { GetCurrentUserUseCase(get()) }
    single { RefreshTokenUseCase(get()) }
}