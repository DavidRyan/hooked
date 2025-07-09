package di

import androidx.activity.ComponentActivity
import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import com.hooked.core.photo.PhotoLaunchers
import com.hooked.catches.data.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { 
        PhotoCapture(androidContext() as ComponentActivity, get<PhotoLaunchers>()) 
    }
    single { 
        ImageProcessor(androidContext().applicationContext) 
    }
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory(androidContext())
    }
}