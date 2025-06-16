package di

import androidx.activity.ComponentActivity
import core.photo.ImageProcessor
import core.photo.PhotoCapture
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { 
        PhotoCapture(androidContext() as ComponentActivity) 
    }
    single { 
        ImageProcessor(androidContext()) 
    }
}