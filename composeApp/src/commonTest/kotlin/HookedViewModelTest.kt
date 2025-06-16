package com.hooked.test

import core.HookedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

// Test data classes
data class TestState(
    val counter: Int = 0,
    val isLoading: Boolean = false
)

sealed class TestIntent {
    object Increment : TestIntent()
    object Decrement : TestIntent()
    object StartLoading : TestIntent()
    object StopLoading : TestIntent()
}

sealed class TestEffect {
    data class ShowMessage(val message: String) : TestEffect()
    object NavigateAway : TestEffect()
}

// Test implementation of HookedViewModel
class TestHookedViewModel : HookedViewModel<TestIntent, TestState, TestEffect>() {
    override fun createInitialState(): TestState = TestState()

    override fun handleIntent(intent: TestIntent) {
        when (intent) {
            TestIntent.Increment -> {
                setState { copy(counter = counter + 1) }
            }
            TestIntent.Decrement -> {
                setState { copy(counter = counter - 1) }
                if (state.value.counter < 0) {
                    sendEffect { TestEffect.ShowMessage("Counter is negative!") }
                }
            }
            TestIntent.StartLoading -> {
                setState { copy(isLoading = true) }
            }
            TestIntent.StopLoading -> {
                setState { copy(isLoading = false) }
                sendEffect { TestEffect.NavigateAway }
            }
        }
    }
}

@ExperimentalCoroutinesApi
class HookedViewModelTest {

    private lateinit var viewModel: TestHookedViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TestHookedViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() {
        val initialState = viewModel.state.value
        assertEquals(0, initialState.counter)
        assertFalse(initialState.isLoading)
    }

    @Test
    fun `test increment intent`() = runTest {
        viewModel.sendIntent(TestIntent.Increment)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(1, state.counter)
        assertFalse(state.isLoading)
    }

    @Test
    fun `test multiple increments`() = runTest {
        repeat(5) {
            viewModel.sendIntent(TestIntent.Increment)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(5, state.counter)
    }

    @Test
    fun `test decrement intent`() = runTest {
        // First increment to 2
        viewModel.sendIntent(TestIntent.Increment)
        viewModel.sendIntent(TestIntent.Increment)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then decrement
        viewModel.sendIntent(TestIntent.Decrement)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(1, state.counter)
    }

    @Test
    fun `test loading state changes`() = runTest {
        viewModel.sendIntent(TestIntent.StartLoading)
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = viewModel.state.value
        assertTrue(state.isLoading)
        
        viewModel.sendIntent(TestIntent.StopLoading)
        testDispatcher.scheduler.advanceUntilIdle()
        
        state = viewModel.state.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `test effect emission on negative counter`() = runTest {
        // Start collecting effects
        val effects = mutableListOf<TestEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }
        
        // Decrement from 0 to trigger negative counter effect
        viewModel.sendIntent(TestIntent.Decrement)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(-1, state.counter)
        
        // Check that the effect was emitted
        assertEquals(1, effects.size)
        assertTrue(effects[0] is TestEffect.ShowMessage)
        assertEquals("Counter is negative!", (effects[0] as TestEffect.ShowMessage).message)
        
        job.cancel()
    }

    @Test
    fun `test effect emission on stop loading`() = runTest {
        val effects = mutableListOf<TestEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }
        
        viewModel.sendIntent(TestIntent.StopLoading)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(1, effects.size)
        assertTrue(effects[0] is TestEffect.NavigateAway)
        
        job.cancel()
    }

    @Test
    fun `test state flow is reactive`() = runTest {
        val states = mutableListOf<TestState>()
        val job = launch {
            viewModel.state.collect { states.add(it) }
        }
        
        // Initial state should be collected
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.sendIntent(TestIntent.Increment)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.sendIntent(TestIntent.StartLoading)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should have initial state + 2 updates
        assertEquals(3, states.size)
        assertEquals(0, states[0].counter)
        assertEquals(1, states[1].counter)
        assertTrue(states[2].isLoading)
        
        job.cancel()
    }

    @Test
    fun `test concurrent intent handling`() = runTest {
        // Send multiple intents rapidly
        viewModel.sendIntent(TestIntent.Increment)
        viewModel.sendIntent(TestIntent.Increment)
        viewModel.sendIntent(TestIntent.Decrement)
        viewModel.sendIntent(TestIntent.Increment)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(2, state.counter) // +1 +1 -1 +1 = 2
    }
}