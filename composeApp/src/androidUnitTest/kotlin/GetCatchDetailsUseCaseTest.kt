package com.hooked.test

import com.hooked.catches.domain.usecases.GetCatchDetailsUseCase
import com.hooked.catches.domain.usecases.GetCatchDetailsUseCaseResult
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.catches.domain.entities.CatchDetailsEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCatchDetailsUseCaseTest {

    private val mockRepository = mockk<CatchRepository>()
    private val useCase = GetCatchDetailsUseCase(mockRepository)

    @Test
    fun `test invoke success`() = runTest {
        val catchId = 123L
        val testEntity = CatchDetailsEntity(
            id = catchId,
            species = "Bass",
            weight = 2.5,
            length = 30.0,
            photoUrl = "test-url",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = 1640995200000L,
            location = "40.7128, -74.0060",
            dateCaught = "Jan 1, 2022"
        )

        coEvery { mockRepository.getCatchDetails(catchId) } returns Result.success(testEntity)

        val result = useCase(catchId)

        assertTrue(result is GetCatchDetailsUseCaseResult.Success)
        assertEquals(catchId, (result as GetCatchDetailsUseCaseResult.Success).catchDetails.id)
        assertEquals("Bass", result.catchDetails.species)
        assertEquals(2.5, result.catchDetails.weight)
        assertEquals("40.7128, -74.0060", result.catchDetails.location)
    }

    @Test
    fun `test invoke error`() = runTest {
        val catchId = 123L
        val errorMessage = "Network error"

        coEvery { mockRepository.getCatchDetails(catchId) } returns Result.failure(Exception(errorMessage))

        val result = useCase(catchId)

        assertTrue(result is GetCatchDetailsUseCaseResult.Error)
        assertEquals(errorMessage, (result as GetCatchDetailsUseCaseResult.Error).message)
    }

    @Test
    fun `test invoke with null coordinates`() = runTest {
        val catchId = 123L
        val testEntity = CatchDetailsEntity(
            id = catchId,
            species = "Trout",
            weight = 1.5,
            length = 25.0,
            photoUrl = "test-url",
            latitude = null,
            longitude = null,
            timestamp = null,
            location = "Unknown location",
            dateCaught = "Unknown date"
        )

        coEvery { mockRepository.getCatchDetails(catchId) } returns Result.success(testEntity)

        val result = useCase(catchId)

        assertTrue(result is GetCatchDetailsUseCaseResult.Success)
        assertEquals("Unknown location", (result as GetCatchDetailsUseCaseResult.Success).catchDetails.location)
        assertEquals("Unknown date", result.catchDetails.dateCaught)
    }
}