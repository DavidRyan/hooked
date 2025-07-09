package di

import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import com.hooked.catches.data.database.DatabaseDriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { PhotoCapture() }
    single { ImageProcessor() }
    single { DatabaseDriverFactory() }
}