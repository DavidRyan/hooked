package di

import com.hooked.catches.presentation.CatchGridViewModel
import com.hooked.catches.presentation.CatchDetailsViewModel
import com.hooked.submit.presentation.SubmitCatchViewModel
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.catches.domain.usecases.GetCatchDetailsUseCase
import com.hooked.submit.domain.usecases.SubmitCatchUseCase
import com.hooked.catches.domain.repositories.CatchRepository as CatchesRepositoryInterface
import com.hooked.submit.domain.repositories.SubmitRepository as SubmitRepositoryInterface
import com.hooked.catches.data.repo.CatchRepositoryImpl
import com.hooked.submit.data.repo.SubmitRepositoryImpl
import com.hooked.catches.data.api.CatchApiService
import com.hooked.submit.data.api.SubmitApiService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
    viewModel { SubmitCatchViewModel(get(), get()) }
}

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
        }
    }

    single<CatchApiService> {
        CatchApiService(get())
    }

    single<SubmitApiService> {
        SubmitApiService(get())
    }

    single<CatchesRepositoryInterface> {
        CatchRepositoryImpl(get())
    }

    single<SubmitRepositoryInterface> {
        SubmitRepositoryImpl(get())
    }
}

val useCaseModule = module {
    single { GetCatchesUseCase(get()) }
    single { GetCatchDetailsUseCase(get()) }
    single { SubmitCatchUseCase(get()) }
}