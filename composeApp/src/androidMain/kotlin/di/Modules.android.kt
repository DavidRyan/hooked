package di

import androidx.activity.ComponentActivity
import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import com.hooked.core.photo.PhotoLaunchers
import com.hooked.core.location.LocationService
import com.hooked.catches.data.database.DatabaseDriverFactory
import com.hooked.auth.data.storage.AndroidSecureTokenStorage
import com.hooked.auth.data.storage.AndroidPreferencesTokenStorage
import com.hooked.auth.data.storage.InMemoryTokenStorage
import com.hooked.auth.data.storage.TokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { 
        PhotoCapture(androidContext() as ComponentActivity, get<PhotoLaunchers>()) 
    }
    single { 
        ImageProcessor(androidContext().applicationContext) 
    }
    single {
        LocationService(androidContext() as ComponentActivity)
    }
    single { 
        DatabaseDriverFactory(androidContext()) 
    }
    // Token Storage - easily swap implementations by changing this line:
    single<TokenStorage> { 
        AndroidSecureTokenStorage(androidContext())           // Secure (EncryptedSharedPreferences)
        // AndroidPreferencesTokenStorage(androidContext())  // Simple (SharedPreferences)
        // InMemoryTokenStorage()                             // In-memory (for testing)
    }
}
