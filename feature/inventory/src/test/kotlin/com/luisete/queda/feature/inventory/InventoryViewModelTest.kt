package com.luisete.queda.feature.inventory

import com.luisete.queda.core.domain.inventory.ObserveExactInventoryItemsUseCase
import com.luisete.queda.core.testing.FakeCurrentHouseholdIdProvider
import com.luisete.queda.core.testing.FakeInventoryRepository
import com.luisete.queda.core.testing.InventoryTestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var useCase: ObserveExactInventoryItemsUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeInventoryRepository()
        useCase = ObserveExactInventoryItemsUseCase(repository, FakeCurrentHouseholdIdProvider())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() =
        runTest(testDispatcher) {
            val viewModel = InventoryViewModel(useCase)
            assertEquals(InventoryUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun emptyRepositoryEmitsEmpty() =
        runTest(testDispatcher) {
            val viewModel = InventoryViewModel(useCase)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            repository.emit(emptyList())
            advanceUntilIdle()

            assertEquals(InventoryUiState.Empty, viewModel.uiState.value)
            job.cancel()
        }

    @Test
    fun nonEmptyRepositoryEmitsContent() =
        runTest(testDispatcher) {
            val viewModel = InventoryViewModel(useCase)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val item = InventoryTestData.createInventoryItem(name = "Milk")
            repository.emit(listOf(item))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Expected Content state, but got $state", state is InventoryUiState.Content)
            assertEquals("Milk", (state as InventoryUiState.Content).items[0].name)
            job.cancel()
        }

    @Test
    fun repositoryFailureEmitsError() =
        runTest(testDispatcher) {
            val viewModel = InventoryViewModel(useCase)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            repository.setFlowError(RuntimeException("Error"))
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(InventoryUiState.Error, viewModel.uiState.value)
            job.cancel()
        }

    @Test
    fun retrySubscribesAgain() =
        runTest(testDispatcher) {
            val viewModel = InventoryViewModel(useCase)
            val job =
                launch {
                    viewModel.uiState.collect {}
                }

            // Initial subscription
            advanceUntilIdle()
            assertEquals(
                "Initial subscription count should be 1",
                1,
                repository.observeSubscriptionsCount,
            )

            // Fail and retry
            repository.setFlowError(RuntimeException("Error"))
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(
                "Should be in Error state after failure",
                InventoryUiState.Error,
                viewModel.uiState.value,
            )
            assertEquals(
                "Subscription count should be 2 after first retry",
                2,
                repository.observeSubscriptionsCount,
            )

            // Recover and retry
            repository.setFlowError(null)
            repository.emit(emptyList())
            viewModel.retry()
            advanceUntilIdle()

            assertEquals("Final state should be Empty", InventoryUiState.Empty, viewModel.uiState.value)
            assertEquals(
                "Subscription count should be 3 after second retry",
                3,
                repository.observeSubscriptionsCount,
            )
            job.cancel()
        }

    @Test
    fun viewModelDoesNotSortRepositoryResult() =
        runTest(testDispatcher) {
            val viewModel = InventoryViewModel(useCase)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val item1 = InventoryTestData.createInventoryItem(name = "Z", productId = "p1")
            val item2 = InventoryTestData.createInventoryItem(name = "A", productId = "p2")
            repository.emit(listOf(item1, item2))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Expected Content state, but got $state", state is InventoryUiState.Content)
            state as InventoryUiState.Content
            assertEquals("Z", state.items[0].name)
            assertEquals("A", state.items[1].name)
            job.cancel()
        }
}
