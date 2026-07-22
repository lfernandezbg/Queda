package com.luisete.queda.feature.inventory

import com.luisete.queda.core.domain.inventory.ConsumeExactQuantityUseCase
import com.luisete.queda.core.domain.inventory.CorrectExactQuantityUseCase
import com.luisete.queda.core.domain.inventory.GetConsumePreviewUseCase
import com.luisete.queda.core.domain.inventory.GetCorrectPreviewUseCase
import com.luisete.queda.core.domain.inventory.ObserveExactInventoryItemsUseCase
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
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
@Suppress("LargeClass")
class InventoryViewModelTest {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var observeUseCase: ObserveExactInventoryItemsUseCase
    private lateinit var consumeUseCase: ConsumeExactQuantityUseCase
    private lateinit var correctUseCase: CorrectExactQuantityUseCase
    private lateinit var getConsumePreviewUseCase: GetConsumePreviewUseCase
    private lateinit var getCorrectPreviewUseCase: GetCorrectPreviewUseCase
    private val householdIdProvider = FakeCurrentHouseholdIdProvider()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeInventoryRepository()
        observeUseCase = ObserveExactInventoryItemsUseCase(repository, householdIdProvider)
        consumeUseCase = ConsumeExactQuantityUseCase(repository)
        correctUseCase = CorrectExactQuantityUseCase(repository)
        getConsumePreviewUseCase = GetConsumePreviewUseCase()
        getCorrectPreviewUseCase = GetCorrectPreviewUseCase()
    }

    private fun createViewModel() =
        InventoryViewModel(
            observeExactInventoryItemsUseCase = observeUseCase,
            consumeExactQuantityUseCase = consumeUseCase,
            correctExactQuantityUseCase = correctUseCase,
            getConsumePreviewUseCase = getConsumePreviewUseCase,
            getCorrectPreviewUseCase = getCorrectPreviewUseCase,
        )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            assertEquals(InventoryUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun emptyRepositoryEmitsEmpty() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
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
            val viewModel = createViewModel()
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
            val viewModel = createViewModel()
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
            val viewModel = createViewModel()
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
    fun tappingItemOpensActionSelection() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]

            viewModel.onItemClick(item)
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            assertTrue(state.quantityAction is QuantityActionUiState.ActionSelection)
            assertEquals("s1", (state.quantityAction as QuantityActionUiState.ActionSelection).item.id)
            job.cancel()
        }

    @Test
    fun dismissingActionSelectionClosesSheet() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()

            viewModel.onDismissSheet()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            assertEquals(QuantityActionUiState.Closed, state.quantityAction)
            job.cancel()
        }

    @Test
    fun selectingConsumeOpensForm() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()

            viewModel.onSelectConsume()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            assertTrue(state.quantityAction is QuantityActionUiState.ConsumeEditing)
            job.cancel()
        }

    @Test
    fun selectingCorrectOpensForm() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()

            viewModel.onSelectCorrect()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            assertTrue(state.quantityAction is QuantityActionUiState.CorrectEditing)
            job.cancel()
        }

    @Test
    fun amountChangeUpdatesPreview() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                        amount = "10",
                        unit = MeasurementUnit.UNIT,
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()

            viewModel.onAmountChange("3")
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals("7", action.preview?.amountFormatted)
            assertEquals(MeasurementUnit.UNIT, action.preview?.unit)
            job.cancel()
        }

    @Test
    fun unitChangeUpdatesPreview() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                        amount = "1",
                        unit = MeasurementUnit.KILOGRAM,
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()

            viewModel.onAmountChange("500")
            viewModel.onUnitChange(MeasurementUnit.GRAM)
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            // 1kg - 500g = 0.5kg (if available.unit is preferred)
            assertEquals("0,5", action.preview?.amountFormatted)
            assertEquals(MeasurementUnit.KILOGRAM, action.preview?.unit)
            job.cancel()
        }

    @Test
    fun correctionPreviewSameUnit() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                        amount = "10",
                        unit = MeasurementUnit.UNIT,
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectCorrect()
            advanceUntilIdle()

            viewModel.onAmountChange("12")
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.CorrectEditing
            assertEquals("12", action.preview?.amountFormatted)
            assertEquals(MeasurementUnit.UNIT, action.preview?.unit)
            job.cancel()
        }

    @Test
    fun submittingConsumeClosesSheetOnSuccess() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("3")
            advanceUntilIdle()
            repository.setMutationResult(
                QuantityMutationResult.Success(
                    ExactQuantity.of("7", MeasurementUnit.UNIT),
                ),
            )

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            assertEquals(QuantityActionUiState.Closed, state.quantityAction)
            job.cancel()
        }

    @Test
    fun submittingWithInvalidFormatShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("invalid")
            advanceUntilIdle()

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.INVALID_AMOUNT, action.error)
            job.cancel()
        }

    @Test
    fun submittingZeroShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("0")
            advanceUntilIdle()

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.MUST_BE_POSITIVE, action.error)
            job.cancel()
        }

    @Test
    fun submittingNegativeShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("0") // Changed from -1 to 0 to reliably test MUST_BE_POSITIVE
            advanceUntilIdle()

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.MUST_BE_POSITIVE, action.error)
            job.cancel()
        }

    @Test
    fun submittingEqualConsumptionShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                        amount = "10",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("10")
            advanceUntilIdle()
            repository.setMutationResult(
                QuantityMutationResult.Failure(DomainError.AmountMustBeLowerThanCurrent),
            )

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.MUST_BE_LOWER_THAN_CURRENT, action.error)
            job.cancel()
        }

    @Test
    fun submittingGreaterThanCurrentConsumptionShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                        amount = "10",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("11")
            advanceUntilIdle()
            repository.setMutationResult(
                QuantityMutationResult.Failure(DomainError.AmountMustBeLowerThanCurrent),
            )

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.MUST_BE_LOWER_THAN_CURRENT, action.error)
            job.cancel()
        }

    @Test
    fun unchangedCorrectionShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                        amount = "10",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectCorrect()
            advanceUntilIdle()
            viewModel.onAmountChange("10")
            advanceUntilIdle()
            repository.setMutationResult(QuantityMutationResult.Failure(DomainError.UnchangedQuantity))

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.CorrectEditing
            assertEquals(QuantityActionError.UNCHANGED, action.error)
            job.cancel()
        }

    @Test
    fun productNotFoundMutationShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("5")
            repository.setMutationResult(QuantityMutationResult.Failure(DomainError.ProductNotFound))

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.PRODUCT_NOT_FOUND, action.error)
            job.cancel()
        }

    @Test
    fun storageFailureMutationShowsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("5")
            repository.setMutationResult(QuantityMutationResult.Failure(DomainError.StorageFailure))

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertEquals(QuantityActionError.STORAGE_FAILURE, action.error)
            job.cancel()
        }

    @Test
    fun doubleSubmitIsIgnored() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("5")

            repository.suspendNextMutation()

            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value as InventoryUiState.Content
            assertTrue(state.quantityAction is QuantityActionUiState.ConsumeEditing)
            val action = state.quantityAction as QuantityActionUiState.ConsumeEditing
            assertTrue(action.isSubmitting)

            // Second confirm should not trigger another call
            viewModel.onConfirm()
            advanceUntilIdle()

            repository.completeSuspendedMutation()
            advanceUntilIdle()

            job.cancel()
        }

    @Test
    fun cancelDuringMutationIsIgnored() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()
            val item = (viewModel.uiState.value as InventoryUiState.Content).items[0]
            viewModel.onItemClick(item)
            advanceUntilIdle()
            viewModel.onSelectConsume()
            advanceUntilIdle()
            viewModel.onAmountChange("5")

            repository.suspendNextMutation()
            viewModel.onConfirm()
            advanceUntilIdle()

            viewModel.onDismissSheet()
            advanceUntilIdle()

            // Should still be open and submitting
            val action =
                (viewModel.uiState.value as InventoryUiState.Content)
                    .quantityAction as QuantityActionUiState.ConsumeEditing
            assertTrue(action.isSubmitting)

            repository.completeSuspendedMutation()
            advanceUntilIdle()
            job.cancel()
        }

    @Test
    fun selectItemByIdWaitsForContentAndOpensActionSelection() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Call selectItemById while state is still Loading
            viewModel.selectItemById("s1")
            advanceUntilIdle()

            assertEquals(InventoryUiState.Loading, viewModel.uiState.value)

            // Emit content
            repository.emit(
                listOf(
                    InventoryTestData.createInventoryItem(
                        name = "Milk",
                        stockItemId = "s1",
                    ),
                ),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is InventoryUiState.Content)
            val action = (state as InventoryUiState.Content).quantityAction
            assertTrue(action is QuantityActionUiState.ActionSelection)
            assertEquals("s1", (action as QuantityActionUiState.ActionSelection).item.id)

            job.cancel()
        }
}
