package com.hooked.di

import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import org.koin.dsl.module

actual val platformModule = module {
    single { PhotoCapture() }
    single { ImageProcessor() }
}