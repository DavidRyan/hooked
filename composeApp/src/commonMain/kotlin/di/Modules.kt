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
import com.hooked.catches.domain.usecases.GetRibbonInsightUseCase
import com.hooked.catches.domain.usecases.ObserveCatchEnrichmentUpdatesUseCase
import com.hooked.catches.domain.repositories.CatchRepository as CatchesRepositoryInterface
import com.hooked.catches.domain.repositories.CatchUpdatesRepository
import com.hooked.catches.data.repo.CatchRepositoryImpl
import com.hooked.catches.data.repo.CatchUpdatesRepositoryImpl
import com.hooked.catches.data.api.CatchApiService
import com.hooked.catches.data.live.AuthProvider
import com.hooked.catches.data.live.BaseUrlProvider
import com.hooked.catches.data.live.CatchEnrichmentUpdatesService
import com.hooked.catches.data.live.DefaultEnrichmentEventDecoder
import com.hooked.catches.data.live.EnrichmentEventDecoder
import com.hooked.catches.data.live.NetworkBaseUrlProvider
import com.hooked.catches.data.live.PhoenixSocketClient
import com.hooked.catches.data.live.TokenStorageAuthProvider
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
import io.ktor.client.plugins.websocket.WebSockets
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
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(get())
            }
            install(AuthInterceptor) {
                tokenStorage = get()
            }
            install(WebSockets)
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

    single<BaseUrlProvider> { NetworkBaseUrlProvider() }
    single<AuthProvider> { TokenStorageAuthProvider(get(), get()) }
    single { PhoenixSocketClient(get(), get(), get()) }
    single<EnrichmentEventDecoder> { DefaultEnrichmentEventDecoder() }
    single { CatchEnrichmentUpdatesService(get<PhoenixSocketClient>(), get<AuthProvider>(), get<EnrichmentEventDecoder>()) }

    single<CatchUpdatesRepository> {
        CatchUpdatesRepositoryImpl(get())
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
    single { GetRibbonInsightUseCase(get()) }
    single { ObserveCatchEnrichmentUpdatesUseCase(get()) }
    single { SubmitSkunkUseCase(get()) }
} + authUseCaseModule
