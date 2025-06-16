package di

import data.api.HookedApiService
import data.repo.CatchRepository as CatchRepositoryImpl
import domain.repository.CatchRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module


val dataModule = module {
    single {
        HttpClient {
            expectSuccess = true
        }
    }

    single<HookedApiService> {
        HookedApiService(get())
    }

    single<CatchRepository> {
        CatchRepositoryImpl(get())
    }
}
