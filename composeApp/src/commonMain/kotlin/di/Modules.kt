package di

import grid.CatchGridViewModel
import details.CatchDetailsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlinx.serialization.json.Json

val sharedModule = module {
/*
    singleOf(::CatchGridRepository)
    singleOf(::CatchDetailsRepository)
    singleOf(::GetCatchesUseCase)
    singleOf(::GetCatchDetailsUseCase)
*/
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
}