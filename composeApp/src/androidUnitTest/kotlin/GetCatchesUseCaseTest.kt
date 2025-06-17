package com.hooked.test

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.catches.domain.usecases.GetCatchesUseCaseResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCatchesUseCaseTest {

    private val mockRepository = mockk<CatchRepository>()
    private val useCase = GetCatchesUseCase(mockRepository)

    @Test
    fun `test invoke success with multiple catches`() = runTest {
        val testCatches = listOf(
            CatchEntity(
                id = 1L,
                name = "Bass",
                description = "A large bass catch",
                dateCaught = "2023-01-01",
                location = "Test Lake",
                imageUrl = "test-url-1",
                weight = 2.5,
                length = 30.0
            ),
            CatchEntity(
                id = 2L,
                name = "Trout",
                description = "A small trout catch",
                dateCaught = "2023-01-02", 
                location = "Test River",
                imageUrl = "test-url-2",
                weight = 1.5,
                length = 25.0
            )
        )

        coEvery { mockRepository.getCatches() } returns Result.success(testCatches)

        val result = useCase()

        assertTrue(result is GetCatchesUseCaseResult.Success)
        val catches = (result as GetCatchesUseCaseResult.Success).catches
        assertEquals(2, catches.size)
        assertEquals("Bass", catches[0].name)
        assertEquals("Trout", catches[1].name)
    }

    @Test
    fun `test invoke success with empty list`() = runTest {
        coEvery { mockRepository.getCatches() } returns Result.success(emptyList())

        val result = useCase()

        assertTrue(result is GetCatchesUseCaseResult.Success)
        val catches = (result as GetCatchesUseCaseResult.Success).catches
        assertEquals(0, catches.size)
    }

    @Test
    fun `test invoke error`() = runTest {
        val errorMessage = "Network error"
        coEvery { mockRepository.getCatches() } returns Result.failure(Exception(errorMessage))

        val result = useCase()

        assertTrue(result is GetCatchesUseCaseResult.Error)
        assertEquals(errorMessage, (result as GetCatchesUseCaseResult.Error).message)
    }

    @Test
    fun `test invoke with single catch`() = runTest {
        val testCatch = CatchEntity(
            id = 1L,
            name = "Salmon",
            description = "A large salmon",
            dateCaught = "2023-01-01",
            location = "Test Location",
            imageUrl = "test-url",
            weight = 5.0,
            length = 45.0
        )

        coEvery { mockRepository.getCatches() } returns Result.success(listOf(testCatch))

        val result = useCase()

        assertTrue(result is GetCatchesUseCaseResult.Success)
        val catches = (result as GetCatchesUseCaseResult.Success).catches
        assertEquals(1, catches.size)
        assertEquals("Salmon", catches[0].name)
        assertEquals(5.0, catches[0].weight)
    }
}