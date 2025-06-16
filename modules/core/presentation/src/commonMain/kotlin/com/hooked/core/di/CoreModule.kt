package com.hooked.core.di

import org.koin.dsl.module

expect val platformModule: org.koin.core.module.Module

val coreModule = module {
    // Include platform-specific modules
    includes(platformModule)
}