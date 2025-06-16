package com.hooked.features.catches.di

import com.hooked.features.catches.presentation.CatchDetailsViewModel
import com.hooked.features.catches.presentation.CatchGridViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val catchesModule = module {
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
}