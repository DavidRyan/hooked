package di

import domain.usecase.GetCatchesUseCase
import org.koin.dsl.module


val useCaseModule = module {
    single { GetCatchesUseCase(get()) }
}