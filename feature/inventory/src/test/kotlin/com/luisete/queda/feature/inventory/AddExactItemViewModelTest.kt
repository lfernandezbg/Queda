package com.luisete.queda.feature.inventory

import androidx.lifecycle.SavedStateHandle
import com.luisete.queda.core.domain.inventory.AddExactInventoryItemUseCase
import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.model.inventory.StockTrackingMode
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeCurrentHouseholdIdProvider
import com.luisete.queda.core.testing.FakeInventoryRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddExactItemViewModelTest {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var useCase: AddExactInventoryItemUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeInventoryRepository()
        useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultUnitIsUnit() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            assertEquals(MeasurementUnit.UNIT, viewModel.uiState.value.selectedUnit)
        }

    @Test
    fun nameChangeUpdatesStateAndClearsNameErrors() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.save()
            advanceUntilIdle()
            assertEquals(NameInputError.BLANK, viewModel.uiState.value.nameError)

            viewModel.onNameChange("Milk")
            assertEquals("Milk", viewModel.uiState.value.nameInput)
            assertNull(viewModel.uiState.value.nameError)
        }

    @Test
    fun quantityChangeUpdatesStateAndClearsQuantityErrors() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.save()
            advanceUntilIdle()
            assertEquals(QuantityInputError.BLANK, viewModel.uiState.value.quantityError)

            viewModel.onQuantityChange("1.5")
            assertEquals("1.5", viewModel.uiState.value.quantityInput)
            assertNull(viewModel.uiState.value.quantityError)
        }

    @Test
    fun nameChangeClearsDuplicateError() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            repository.setAddResult(AddExactItemRepositoryResult.DuplicateProductName)
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")
            viewModel.save()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.duplicateError)

            viewModel.onNameChange("Leche")
            assertEquals(false, viewModel.uiState.value.duplicateError)
        }

    @Test
    fun inputChangeClearsStorageError() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            repository.setAddResult(AddExactItemRepositoryResult.StorageFailure)
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")
            viewModel.save()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.storageError)

            viewModel.onNameChange("Leche")
            assertEquals(false, viewModel.uiState.value.storageError)
        }

    @Test
    fun unitChangeUpdatesSelectedUnit() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.onUnitChange(MeasurementUnit.LITER)
            assertEquals(MeasurementUnit.LITER, viewModel.uiState.value.selectedUnit)
        }

    @Test
    fun validSubmitSetsSavingAndInvokesUseCaseOnce() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")
            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, repository.addCallsCount)
        }

    @Test
    fun successfulSubmitEmitsSingleNavigationEvent() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")

            val results = mutableListOf<Unit>()
            val job =
                launch {
                    viewModel.successEvent.collect { results.add(it) }
                }

            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, results.size)
            job.cancel()
        }

    @Test
    fun saveWithEmptyFormShowsBothErrors() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.save()
            advanceUntilIdle()
            assertEquals(NameInputError.BLANK, viewModel.uiState.value.nameError)
            assertEquals(QuantityInputError.BLANK, viewModel.uiState.value.quantityError)
            assertEquals(0, repository.addCallsCount)
        }

    @Test
    fun saveWithEmptyNameAndValidQuantityDoesNotInsert() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.onQuantityChange("1")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(NameInputError.BLANK, viewModel.uiState.value.nameError)
            assertEquals(0, repository.addCallsCount)
        }

    @Test
    fun duplicateShowsDuplicateError() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            repository.setAddResult(AddExactItemRepositoryResult.DuplicateProductName)
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")
            viewModel.save()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.duplicateError)
        }

    @Test
    fun storageFailureKeepsFormAndShowsError() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            repository.setAddResult(AddExactItemRepositoryResult.StorageFailure)
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")
            viewModel.save()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.storageError)
            assertEquals("Milk", viewModel.uiState.value.nameInput)
        }

    @Test
    fun secondSubmitWhileSavingIsIgnored() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            repository.suspendNextMutation()
            viewModel.onNameChange("Milk")
            viewModel.onQuantityChange("1")

            val results = mutableListOf<Unit>()
            val job =
                launch {
                    viewModel.successEvent.collect { results.add(it) }
                }

            viewModel.save()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSaving)
            assertEquals(1, repository.addCallsCount)

            viewModel.save()
            advanceUntilIdle()
            assertEquals(1, repository.addCallsCount)

            repository.completeSuspendedMutation()
            advanceUntilIdle()

            assertEquals(1, repository.addCallsCount)
            assertEquals(1, results.size)
            job.cancel()
        }

    @Test
    fun savedStateHandleRestoresNameQuantityUnitAndTrackingMode() =
        runTest(testDispatcher) {
            val handle =
                SavedStateHandle(
                    mapOf(
                        "name" to "Saved",
                        "quantity" to "5",
                        "unit" to MeasurementUnit.GRAM,
                        "trackingMode" to StockTrackingMode.PRESENCE,
                    ),
                )
            val restoredViewModel = AddExactItemViewModel(useCase, handle)

            assertEquals("Saved", restoredViewModel.uiState.value.nameInput)
            assertEquals("5", restoredViewModel.uiState.value.quantityInput)
            assertEquals(MeasurementUnit.GRAM, restoredViewModel.uiState.value.selectedUnit)
            assertEquals(StockTrackingMode.PRESENCE, restoredViewModel.uiState.value.trackingMode)
        }

    @Test
    fun changingToPresenceClearsQuantityErrors() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.save()
            advanceUntilIdle()
            assertEquals(QuantityInputError.BLANK, viewModel.uiState.value.quantityError)

            viewModel.onTrackingModeChange(StockTrackingMode.PRESENCE)
            assertNull(viewModel.uiState.value.quantityError)
        }

    @Test
    fun savingPresenceDoesNotValidateQuantity() =
        runTest(testDispatcher) {
            val viewModel = AddExactItemViewModel(useCase, SavedStateHandle())
            viewModel.onNameChange("Salt")
            viewModel.onTrackingModeChange(StockTrackingMode.PRESENCE)
            viewModel.onQuantityChange("") // Blank but should be ignored

            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, repository.addCallsCount)
            val results = mutableListOf<Unit>()
            val job = launch { viewModel.successEvent.collect { results.add(it) } }
            advanceUntilIdle()
            assertEquals(1, results.size)
            job.cancel()
        }
}
