package com.hooked.di

import com.hooked.features.catches.presentation.CatchGridViewModel
import com.hooked.features.catches.presentation.CatchDetailsViewModel
import com.hooked.features.submit.presentation.SubmitCatchViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
    viewModelOf(::SubmitCatchViewModel)
}