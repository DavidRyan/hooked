package di

import grid.CatchGridViewModel
import details.CatchDetailsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::CatchGridViewModel)
    viewModelOf(::CatchDetailsViewModel)
}