package di

import com.hooked.data.CatchDetailsRepository
import com.hooked.data.CatchGridRepository
import com.hooked.domain.usecase.GetCatchDetailsUseCase
import com.hooked.domain.usecase.GetCatchesUseCase
import grid.CatchGridViewModel
import details.CatchDetailsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

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
    singleOf(::GetCatchesUseCase)
    singleOf(::GetCatchDetailsUseCase)
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
}