package com.hooked.test

import com.hooked.domain.usecase.GetCatchesUseCase
import com.hooked.grid.CatchGridViewModel
import com.hooked.domain.CatchGridIntent
import com.hooked.domain.CatchModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class CatchGridViewModelTest : KoinTest {

    private val viewModel: CatchGridViewModel by inject()
    private val getCatchesUseCase: GetCatchesUseCase by inject()

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single {
                        GetCatchesUseCase(
                            mock {
                                onBlocking { invoke() } doReturn listOf(
                                    CatchModel(1, "Salmon", 5.0, 20.0, "url")
                                )
                            }
                        )
                    }
                    single { CatchGridViewModel(get()) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `test load catches`() = runTest {
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.catches.size)
    }
}
