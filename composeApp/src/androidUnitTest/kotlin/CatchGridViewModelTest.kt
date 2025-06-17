package com.hooked.test

import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.catches.domain.usecases.GetCatchesUseCaseResult
import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.presentation.CatchGridViewModel
import com.hooked.catches.presentation.model.CatchGridIntent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CatchGridViewModelTest {

    private lateinit var mockUseCase: GetCatchesUseCase
    private lateinit var viewModel: CatchGridViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = CatchGridViewModel(mockUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() {
        val state = viewModel.state.value
        assertTrue(state.catches.isEmpty())
        assertTrue(state.isLoading)
    }

    @Test
    fun `test load catches success`() = runTest {
        val testCatches = listOf(
            CatchEntity(1, "Salmon", "A large salmon", "2023-10-01", "Lake", "url1", 5.0, 20.0),
            CatchEntity(2, "Trout", "A small trout", "2023-10-02", "River", "url2", 3.0, 15.0)
        )
        coEvery { mockUseCase() } returns GetCatchesUseCaseResult.Success(testCatches)
        
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(2, state.catches.size)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `test load catches shows loading state`() = runTest {
        coEvery { mockUseCase() } returns GetCatchesUseCaseResult.Success(emptyList())
        
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        
        assertTrue(viewModel.state.value.isLoading)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
    }
    
    @Test
    fun `test load catches error`() = runTest {
        coEvery { mockUseCase() } returns GetCatchesUseCaseResult.Error("Network error")
        
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state.catches.isEmpty())
        assertFalse(state.isLoading)
    }
}
