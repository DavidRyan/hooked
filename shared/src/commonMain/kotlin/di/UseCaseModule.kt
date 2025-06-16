package di

import domain.usecase.GetCatchesUseCase
import domain.usecase.GetCatchDetailsUseCase
import org.koin.dsl.module


val useCaseModule = module {
    single { GetCatchesUseCase(get()) }
    single { GetCatchDetailsUseCase(get()) }
}