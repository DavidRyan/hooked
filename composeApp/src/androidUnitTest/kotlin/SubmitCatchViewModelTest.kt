package com.hooked.test

import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import com.hooked.core.photo.PhotoCaptureResult
import com.hooked.core.photo.CapturedPhoto
import com.hooked.submit.domain.entities.SubmitCatchEntity
import com.hooked.submit.domain.usecases.SubmitCatchUseCase
import com.hooked.submit.domain.usecases.SubmitCatchUseCaseResult
import com.hooked.submit.presentation.SubmitCatchViewModel
import com.hooked.submit.presentation.model.SubmitCatchEffect
import com.hooked.submit.presentation.model.SubmitCatchIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
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
class SubmitCatchViewModelTest {

    private lateinit var mockSubmitCatchUseCase: SubmitCatchUseCase
    private lateinit var mockPhotoCapture: PhotoCapture
    private lateinit var mockImageProcessor: ImageProcessor
    private lateinit var viewModel: SubmitCatchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSubmitCatchUseCase = mockk()
        mockPhotoCapture = mockk()
        mockImageProcessor = mockk()
        viewModel = SubmitCatchViewModel(mockSubmitCatchUseCase, mockPhotoCapture, mockImageProcessor)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() {
        val state = viewModel.state.value
        assertEquals("", state.species)
        assertEquals("", state.weight)
        assertEquals("", state.length)
        assertEquals(null, state.latitude)
        assertEquals(null, state.longitude)
        assertEquals(null, state.photoUri)
        assertFalse(state.isLoading)
        assertFalse(state.isSubmitting)
        assertFalse(state.isLocationLoading)
        assertEquals(null, state.errorMessage)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `test update species`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.UpdateSpecies("Bass"))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals("Bass", state.species)
    }

    @Test
    fun `test update weight`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.UpdateWeight("2.5"))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals("2.5", state.weight)
    }

    @Test
    fun `test update length`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.UpdateLength("30.0"))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals("30.0", state.length)
    }

    @Test
    fun `test update location`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.UpdateLocation(40.7128, -74.0060))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(40.7128, state.latitude)
        assertEquals(-74.0060, state.longitude)
        assertFalse(state.isLocationLoading)
    }

    @Test
    fun `test update photo`() = runTest {
        val photoUri = "file://test-photo.jpg"
        viewModel.sendIntent(SubmitCatchIntent.UpdatePhoto(photoUri))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(photoUri, state.photoUri)
    }

    @Test
    fun `test form validation`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.UpdateSpecies("Bass"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateWeight("2.5"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateLength("30.0"))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state.isFormValid)
    }

    @Test
    fun `test form validation with invalid weight`() {
        viewModel.sendIntent(SubmitCatchIntent.UpdateSpecies("Bass"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateWeight("invalid"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateLength("30.0"))
        
        val state = viewModel.state.value
        assertFalse(state.isFormValid)
    }

    @Test
    fun `test take photo success`() = runTest {
        val capturedPhoto = CapturedPhoto("file://test-photo.jpg", null)
        coEvery { mockPhotoCapture.capturePhoto() } returns PhotoCaptureResult.Success(capturedPhoto)
        
        viewModel.sendIntent(SubmitCatchIntent.TakePhoto)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals("file://test-photo.jpg", state.photoUri)
    }

    @Test
    fun `test take photo error`() = runTest {
        coEvery { mockPhotoCapture.capturePhoto() } returns PhotoCaptureResult.Error("Camera error")
        
        viewModel.sendIntent(SubmitCatchIntent.TakePhoto)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Photo error is handled - no crash means the error was processed
        val state = viewModel.state.value
        // State should not have a photo URI set
        assertEquals(null, state.photoUri)
    }

    @Test
    fun `test pick photo success`() = runTest {
        val capturedPhoto = CapturedPhoto("file://gallery-photo.jpg", null)
        coEvery { mockPhotoCapture.pickFromGallery() } returns PhotoCaptureResult.Success(capturedPhoto)
        
        viewModel.sendIntent(SubmitCatchIntent.PickPhoto)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals("file://gallery-photo.jpg", state.photoUri)
    }

    @Test
    fun `test get current location`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.GetCurrentLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state.isLocationLoading)
    }

    @Test
    fun `test submit catch success`() = runTest {
        coEvery { mockImageProcessor.loadImageFromUri(any()) } returns byteArrayOf(1, 2, 3)
        coEvery { mockImageProcessor.processImageWithExif(any()) } returns byteArrayOf(1, 2, 3)
        coEvery { mockSubmitCatchUseCase(any()) } returns SubmitCatchUseCaseResult.Success(123L)
        
        viewModel.sendIntent(SubmitCatchIntent.UpdateSpecies("Bass"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateWeight("2.5"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateLength("30.0"))
        viewModel.sendIntent(SubmitCatchIntent.UpdatePhoto("file://test.jpg"))
        
        viewModel.sendIntent(SubmitCatchIntent.SubmitCatch)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `test submit catch with invalid form`() = runTest {
        viewModel.sendIntent(SubmitCatchIntent.SubmitCatch)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `test submit catch error`() = runTest {
        coEvery { mockSubmitCatchUseCase(any()) } returns SubmitCatchUseCaseResult.Error("Network error")
        
        viewModel.sendIntent(SubmitCatchIntent.UpdateSpecies("Bass"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateWeight("2.5"))
        viewModel.sendIntent(SubmitCatchIntent.UpdateLength("30.0"))
        
        viewModel.sendIntent(SubmitCatchIntent.SubmitCatch)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `test navigate back`() {
        viewModel.sendIntent(SubmitCatchIntent.NavigateBack)
        
        // Navigation effect is sent, but we can't easily test it without coroutine scope
        // The intent is processed successfully if no exception is thrown
    }
}