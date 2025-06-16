package com.hooked.test

import data.model.CatchDto
import data.model.CatchDetailsResult
import data.repo.CatchRepository
import domain.usecase.GetCatchDetailsUseCase
import domain.usecase.GetCatchDetailsUseCaseResult
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
        val testDto = CatchDto(
            id = catchId,
            species = "Bass",
            weight = 2.5,
            length = 30.0,
            photoUrl = "test-url",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = 1640995200000L
        )

        coEvery { mockRepository.getCatchDetails(catchId) } returns CatchDetailsResult.Success(testDto)

        val result = useCase(catchId)

        assertTrue(result is GetCatchDetailsUseCaseResult.Success)
        assertEquals(catchId, result.catchDetails.id)
        assertEquals("Bass", result.catchDetails.species)
        assertEquals(2.5, result.catchDetails.weight)
        assertEquals("40.7128, -74.0060", result.catchDetails.location)
    }

    @Test
    fun `test invoke error`() = runTest {
        val catchId = 123L
        val errorMessage = "Network error"

        coEvery { mockRepository.getCatchDetails(catchId) } returns CatchDetailsResult.Error(errorMessage)

        val result = useCase(catchId)

        assertTrue(result is GetCatchDetailsUseCaseResult.Error)
        assertEquals(errorMessage, result.message)
    }

    @Test
    fun `test invoke with null coordinates`() = runTest {
        val catchId = 123L
        val testDto = CatchDto(
            id = catchId,
            species = "Trout",
            weight = 1.5,
            length = 25.0,
            photoUrl = "test-url",
            latitude = null,
            longitude = null,
            timestamp = null
        )

        coEvery { mockRepository.getCatchDetails(catchId) } returns CatchDetailsResult.Success(testDto)

        val result = useCase(catchId)

        assertTrue(result is GetCatchDetailsUseCaseResult.Success)
        assertEquals("Unknown location", result.catchDetails.location)
        assertEquals("Unknown date", result.catchDetails.dateCaught)
    }
}