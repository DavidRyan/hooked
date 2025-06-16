package com.hooked.test

import com.hooked.catches.presentation.details.CatchDetailsViewModel
import com.hooked.catches.presentation.details.model.CatchDetailsIntent
import com.hooked.catches.presentation.details.model.CatchDetailsModel
import com.hooked.catches.data.usecases.GetCatchDetailsUseCase
import com.hooked.catches.data.usecases.GetCatchDetailsUseCaseResult
import com.hooked.catches.domain.model.CatchDetailsEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class CatchDetailsViewModelTest {

    private lateinit var mockUseCase: GetCatchDetailsUseCase
    private lateinit var viewModel: CatchDetailsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = CatchDetailsViewModel(mockUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() {
        val state = viewModel.state.value
        assertNull(state.catchDetails)
        assertTrue(state.isLoading)
    }

    @Test
    fun `test load catch details success`() = runTest {
        val catchId = 123L
        val testEntity = CatchDetailsEntity(
            id = catchId,
            species = "Bass",
            weight = 2.5,
            length = 30.0,
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = 1640995200000L,
            photoUrl = "test-url",
            location = "Test Lake",
            dateCaught = "Jan 1, 2022"
        )
        
        coEvery { mockUseCase(catchId) } returns GetCatchDetailsUseCaseResult.Success(testEntity)
        
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.catchDetails)
        assertEquals(catchId, state.catchDetails?.id)
        assertEquals("Bass", state.catchDetails?.species)
        assertEquals(2.5, state.catchDetails?.weight)
    }

    @Test
    fun `test load catch details error`() = runTest {
        val catchId = 123L
        val errorMessage = "Network error"
        
        coEvery { mockUseCase(catchId) } returns GetCatchDetailsUseCaseResult.Error(errorMessage)
        
        val effects = mutableListOf<com.hooked.catches.presentation.details.model.CatchDetailsEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }
        
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.catchDetails)
        
        assertEquals(1, effects.size)
        assertTrue(effects[0] is com.hooked.catches.presentation.details.model.CatchDetailsEffect.OnError)
        assertEquals(errorMessage, (effects[0] as com.hooked.catches.presentation.details.model.CatchDetailsEffect.OnError).message)
        
        job.cancel()
    }

    @Test
    fun `test loading state during fetch`() = runTest {
        val catchId = 123L
        
        coEvery { mockUseCase(catchId) } returns GetCatchDetailsUseCaseResult.Success(
            CatchDetailsEntity(
                id = catchId,
                species = "Test",
                weight = 1.0,
                length = 10.0,
                latitude = null,
                longitude = null,
                timestamp = null,
                photoUrl = "url"
            )
        )
        
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
        
        assertTrue(viewModel.state.value.isLoading)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
    }
}