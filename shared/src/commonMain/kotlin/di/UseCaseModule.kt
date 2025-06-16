package di

import domain.usecase.GetCatchesUseCase
import domain.usecase.GetCatchDetailsUseCase
import domain.usecase.SubmitCatchUseCase
import org.koin.dsl.module


val useCaseModule = module {
    single { GetCatchesUseCase(get()) }
    single { GetCatchDetailsUseCase(get()) }
    single { SubmitCatchUseCase(get()) }
}