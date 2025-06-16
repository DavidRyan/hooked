package com.hooked.test

import details.CatchDetailsViewModel
import details.model.CatchDetailsIntent
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CatchDetailsViewModelTest : KoinTest {

    private val viewModel: CatchDetailsViewModel by inject()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single { CatchDetailsViewModel() }
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
    fun `test initial state`() {
        val state = viewModel.state.value
        assertNull(state.catchDetails)
        assertTrue(state.isLoading)
    }

    @Test
    fun `test load catch details intent`() = runTest {
        val catchId = 123L
        
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Since the implementation is commented out, state should remain unchanged
        val state = viewModel.state.value
        assertNull(state.catchDetails)
        assertTrue(state.isLoading)
    }

    // Note: createInitialState() is protected, so we can't test it directly
    // The initial state is already tested in the "test initial state" test
}