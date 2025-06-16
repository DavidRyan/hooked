package com.hooked.di

import di.dataModule
import di.useCaseModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(dataModule, useCaseModule, presentationModule, platformModule)
    }
}

expect val platformModule: org.koin.core.module.Module