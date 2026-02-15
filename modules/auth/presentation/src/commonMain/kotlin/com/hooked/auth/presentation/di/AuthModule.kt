package com.hooked.auth.presentation.di

import com.hooked.auth.presentation.LoginViewModel
import com.hooked.auth.presentation.CreateAccountViewModel
import com.hooked.auth.presentation.ProfileViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::CreateAccountViewModel)
    viewModelOf(::ProfileViewModel)
}