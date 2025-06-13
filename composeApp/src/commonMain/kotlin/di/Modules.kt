package di

import details.repository.CatchDetailsRepository
import grid.CatchGridViewModel
import details.CatchDetailsViewModel
import grid.repository.CatchGridRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

//expect val platformModule: Module

val sharedModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
    singleOf(::CatchGridRepository)
    singleOf(::CatchDetailsRepository)
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
}