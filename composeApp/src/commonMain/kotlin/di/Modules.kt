package di

import com.hooked.auth.data.di.authDataModule
import com.hooked.auth.domain.di.authUseCaseModule
import com.hooked.auth.presentation.di.authPresentationModule
import com.hooked.catches.presentation.CatchGridViewModel
import com.hooked.catches.presentation.CatchDetailsViewModel
import com.hooked.catches.presentation.SubmitCatchViewModel
import com.hooked.catches.presentation.StatsViewModel
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.catches.domain.usecases.GetCatchDetailsUseCase
import com.hooked.catches.domain.usecases.SubmitCatchUseCase
import com.hooked.catches.domain.usecases.DeleteCatchUseCase
import com.hooked.catches.domain.usecases.GetCatchStatsUseCase
import com.hooked.catches.domain.usecases.GetFishingInsightsUseCase
import com.hooked.catches.domain.repositories.CatchRepository as CatchesRepositoryInterface
import com.hooked.catches.data.repo.CatchRepositoryImpl
import com.hooked.catches.data.api.CatchApiService
import com.hooked.catches.data.database.DatabaseDriverFactory
import com.hooked.catches.data.database.DatabaseModule
import com.hooked.catches.data.database.CatchLocalDataSource
import com.hooked.auth.data.api.AuthInterceptor
import com.hooked.core.presentation.toast.ToastManager
import com.hooked.skunks.data.api.SkunkApiService
import com.hooked.skunks.data.repo.SkunkRepositoryImpl
import com.hooked.skunks.domain.repositories.SkunkRepository
import com.hooked.skunks.domain.usecases.SubmitSkunkUseCase
import com.hooked.skunks.presentation.SubmitSkunkViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    single { ToastManager() }
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
    viewModel { SubmitCatchViewModel(get(), get()) }
    viewModel { StatsViewModel(get(), get()) }
    viewModel { SubmitSkunkViewModel(get(), get()) }
} + authPresentationModule

val dataModule = module {
    single {
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(AuthInterceptor) {
                tokenStorage = get()
            }
        }
    }

    single<CatchApiService> {
        CatchApiService(get())
    }

    single<SkunkApiService> {
        SkunkApiService(get())
    }

    single { DatabaseModule(get()) }
    
    single<CatchLocalDataSource> {
        get<DatabaseModule>().provideCatchLocalDataSource()
    }

    single<CatchesRepositoryInterface> {
        CatchRepositoryImpl(get(), get())
    }

    single<SkunkRepository> {
        SkunkRepositoryImpl(get())
    }
} + authDataModule

val useCaseModule = module {
    single { GetCatchesUseCase(get()) }
    single { GetCatchDetailsUseCase(get()) }
    single { SubmitCatchUseCase(get()) }
    single { DeleteCatchUseCase(get()) }
    single { GetCatchStatsUseCase(get()) }
    single { GetFishingInsightsUseCase(get()) }
    single { SubmitSkunkUseCase(get()) }
} + authUseCaseModule
