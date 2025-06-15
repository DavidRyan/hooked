package di

import data.api.HookedApiService
import data.repo.CatchRepository
import domain.repository.CatchRepositoy
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
        CatchRepository(get())
    }
}
