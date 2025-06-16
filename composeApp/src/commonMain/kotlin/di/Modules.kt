package di

import com.hooked.catches.presentation.CatchGridViewModel
import com.hooked.catches.presentation.CatchDetailsViewModel
import com.hooked.submit.presentation.SubmitCatchViewModel
import com.hooked.catches.data.usecases.GetCatchesUseCase
import com.hooked.catches.data.usecases.GetCatchDetailsUseCase
import com.hooked.submit.data.usecases.SubmitCatchUseCase
import com.hooked.catches.data.repo.CatchRepository as CatchesRepository
import com.hooked.submit.data.repo.CatchRepository as SubmitRepository
import com.hooked.catches.data.api.CatchApiService
import com.hooked.submit.data.api.SubmitApiService
import io.ktor.client.HttpClient
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
    viewModelOf(::SubmitCatchViewModel)
}

val dataModule = module {
    single {
        HttpClient {
            expectSuccess = true
        }
    }

    single<CatchApiService> {
        CatchApiService(get())
    }

    single<SubmitApiService> {
        SubmitApiService(get())
    }

    single<CatchesRepository> {
        CatchesRepository(get())
    }

    single<SubmitRepository> {
        SubmitRepository(get())
    }
}

val useCaseModule = module {
    single { GetCatchesUseCase(get()) }
    single { GetCatchDetailsUseCase(get()) }
    single { SubmitCatchUseCase(get()) }
}