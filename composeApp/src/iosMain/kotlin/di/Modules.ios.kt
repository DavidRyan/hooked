package di

import core.photo.ImageProcessor
import core.photo.PhotoCapture
import org.koin.dsl.module

actual val platformModule = module {
    single { PhotoCapture() }
    single { ImageProcessor() }
}