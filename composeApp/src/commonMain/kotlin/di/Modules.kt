package di

import grid.CatchGridViewModel
import details.CatchDetailsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
/*
    singleOf(::CatchGridRepository)
    singleOf(::CatchDetailsRepository)
    singleOf(::GetCatchesUseCase)
    singleOf(::GetCatchDetailsUseCase)
*/
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
    // add the use case needed
}