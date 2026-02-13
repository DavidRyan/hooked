package di

import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import com.hooked.core.location.LocationService
import com.hooked.catches.data.database.DatabaseDriverFactory
import com.hooked.auth.data.storage.IosSecureTokenStorage
import com.hooked.auth.data.storage.InMemoryTokenStorage
import com.hooked.auth.data.storage.TokenStorage
import org.koin.dsl.module

actual val platformModule = module {
    single { PhotoCapture() }
    single { ImageProcessor() }
    single { LocationService() }
    single { DatabaseDriverFactory() }
    // Token Storage - easily swap implementations by changing this line:
    single<TokenStorage> { 
        IosSecureTokenStorage()     // Secure (UserDefaults)
        // InMemoryTokenStorage()   // In-memory (for testing)
    }
}