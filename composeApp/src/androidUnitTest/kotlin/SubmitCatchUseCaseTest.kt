package com.hooked.test

import com.hooked.submit.domain.entities.SubmitCatchEntity
import com.hooked.submit.domain.repositories.SubmitRepository
import com.hooked.submit.domain.usecases.SubmitCatchUseCase
import com.hooked.core.domain.UseCaseResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitCatchUseCaseTest {

    private val mockRepository = mockk<SubmitRepository>()
    private val useCase = SubmitCatchUseCase(mockRepository)

    @Test
    fun `test invoke success`() = runTest {
        val entity = SubmitCatchEntity(
            species = "Bass",
            weight = 2.5,
            length = 30.0,
            latitude = 40.7128,
            longitude = -74.0060,
            photoBytes = byteArrayOf(1, 2, 3),
            timestamp = 1640995200000L
        )
        
        val catchId = 123L
        coEvery { mockRepository.submitCatch(entity) } returns Result.success(catchId)

        val result = useCase(entity)

        assertTrue(result is UseCaseResult.Success)
        assertEquals(catchId, (result as UseCaseResult.Success).data)
    }

    @Test
    fun `test invoke error`() = runTest {
        val entity = SubmitCatchEntity(
            species = "Bass",
            weight = 2.5,
            length = 30.0,
            latitude = null,
            longitude = null,
            photoBytes = null,
            timestamp = 0L
        )
        
        val errorMessage = "Network error"
        coEvery { mockRepository.submitCatch(entity) } returns Result.failure(Exception(errorMessage))

        val result = useCase(entity)

        assertTrue(result is UseCaseResult.Error)
        assertEquals(errorMessage, (result as UseCaseResult.Error).message)
    }

    @Test
    fun `test invoke with minimal data`() = runTest {
        val entity = SubmitCatchEntity(
            species = "Trout",
            weight = 1.0,
            length = 20.0,
            latitude = null,
            longitude = null,
            photoBytes = null,
            timestamp = 0L
        )
        
        val catchId = 456L
        coEvery { mockRepository.submitCatch(entity) } returns Result.success(catchId)

        val result = useCase(entity)

        assertTrue(result is UseCaseResult.Success)
        assertEquals(catchId, (result as UseCaseResult.Success).data)
    }

    @Test
    fun `test invoke with exception`() = runTest {
        val entity = SubmitCatchEntity(
            species = "Bass",
            weight = 2.5,
            length = 30.0,
            latitude = 40.7128,
            longitude = -74.0060,
            photoBytes = byteArrayOf(4, 5, 6),
            timestamp = 1640995200000L
        )
        
        val exception = Exception("Connection timeout")
        coEvery { mockRepository.submitCatch(entity) } returns Result.failure(exception)

        val result = useCase(entity)

        assertTrue(result is SubmitCatchUseCaseResult.Error)
        assertEquals("Connection timeout", (result as SubmitCatchUseCaseResult.Error).message)
    }
}
