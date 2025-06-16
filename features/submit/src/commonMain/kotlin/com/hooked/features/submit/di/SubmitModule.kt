package com.hooked.features.submit.di

import com.hooked.features.submit.presentation.SubmitCatchViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val submitModule = module {
    viewModelOf(::SubmitCatchViewModel)
}