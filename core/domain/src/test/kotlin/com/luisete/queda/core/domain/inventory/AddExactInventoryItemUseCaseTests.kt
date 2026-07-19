package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeCurrentHouseholdIdProvider
import com.luisete.queda.core.testing.FakeInventoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class AddExactInventoryItemUseCaseTests {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var householdProvider: FakeCurrentHouseholdIdProvider
    private lateinit var useCase: AddExactInventoryItemUseCase

    @Before
    fun setUp() {
        repository = FakeInventoryRepository()
        householdProvider = FakeCurrentHouseholdIdProvider()
        useCase = AddExactInventoryItemUseCase(repository, householdProvider)
    }

    @Test
    fun validUnitItemIsAdded() {
        runTest {
            val result = useCase("Eggs", "6", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertEquals("Eggs", result.inventoryItem.product.name.displayValue)
            assertEquals(0, result.inventoryItem.stockItem.quantity.amount.compareTo(BigDecimal("6")))
            assertEquals(1, repository.addCallsCount)
        }
    }

    @Test
    fun validGramItemIsAdded() {
        runTest {
            val result = useCase("Flour", "500", MeasurementUnit.GRAM) as AddExactInventoryItemResult.Added
            assertEquals(MeasurementUnit.GRAM, result.inventoryItem.stockItem.quantity.unit)
        }
    }

    @Test
    fun validKilogramItemWithDotIsAdded() {
        runTest {
            val result = useCase("Rice", "1.5", MeasurementUnit.KILOGRAM) as AddExactInventoryItemResult.Added
            assertEquals(0, result.inventoryItem.stockItem.quantity.amount.compareTo(BigDecimal("1.5")))
        }
    }

    @Test
    fun validLiterItemWithCommaIsAdded() {
        runTest {
            val result = useCase("Milk", "1,25", MeasurementUnit.LITER) as AddExactInventoryItemResult.Added
            assertEquals(0, result.inventoryItem.stockItem.quantity.amount.compareTo(BigDecimal("1.25")))
        }
    }

    @Test
    fun validMilliliterItemWithThreeDecimalsIsAdded() {
        runTest {
            val result = useCase("Water", "0.001", MeasurementUnit.MILLILITER) as AddExactInventoryItemResult.Added
            assertEquals(0, result.inventoryItem.stockItem.quantity.amount.compareTo(BigDecimal("0.001")))
        }
    }

    @Test
    fun nameIsTrimmedBeforeRepositoryCall() {
        runTest {
            useCase("  Bread  ", "1", MeasurementUnit.UNIT)
            assertEquals("Bread", repository.addedProducts[0].name.displayValue)
        }
    }

    @Test
    fun repeatedSpacesAreCollapsedBeforeRepositoryCall() {
        runTest {
            useCase("White   Bread", "1", MeasurementUnit.UNIT)
            assertEquals("White Bread", repository.addedProducts[0].name.displayValue)
        }
    }

    @Test
    fun blankNameAndQuantityReturnsBothErrors() {
        runTest {
            val result = useCase("", "", MeasurementUnit.UNIT) as AddExactInventoryItemResult.InvalidInput
            assertEquals(ProductNameCreationError.Blank, result.nameReason)
            assertEquals(ExactQuantityInputError.Blank, result.quantityReason)
            assertEquals(0, repository.addCallsCount)
            assertEquals(0, householdProvider.callCount)
        }
    }

    @Test
    fun invalidNameWithValidQuantityReturnsNameError() {
        runTest {
            val result = useCase("", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.InvalidInput
            assertEquals(ProductNameCreationError.Blank, result.nameReason)
            assertEquals(null, result.quantityReason)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun validNameWithInvalidQuantityReturnsQuantityError() {
        runTest {
            val result = useCase("Milk", "0", MeasurementUnit.LITER) as AddExactInventoryItemResult.InvalidInput
            assertEquals(null, result.nameReason)
            assertEquals(ExactQuantityInputError.NotPositive, result.quantityReason)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun errorDoesNotCallHouseholdProvider() {
        runTest {
            useCase("Milk", "0", MeasurementUnit.UNIT)
            assertEquals(0, householdProvider.callCount)
        }
    }

    @Test
    fun invalidQuantityDoesNotCallRepository() {
        runTest {
            useCase("Name", "abc", MeasurementUnit.UNIT)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun zeroQuantityDoesNotCallRepository() {
        runTest {
            useCase("Name", "0", MeasurementUnit.UNIT)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun tooManyDecimalsDoNotCallRepository() {
        runTest {
            useCase("Name", "1.2345", MeasurementUnit.UNIT)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun duplicateNameIsMapped() {
        runTest {
            repository.setAddResult(AddExactItemRepositoryResult.DuplicateProductName)
            val result = useCase("Milk", "1", MeasurementUnit.LITER)
            assertEquals(AddExactInventoryItemResult.DuplicateProductName, result)
        }
    }

    @Test
    fun storageFailureIsMapped() {
        runTest {
            repository.setAddResult(AddExactItemRepositoryResult.StorageFailure)
            val result = useCase("Milk", "1", MeasurementUnit.LITER)
            assertEquals(AddExactInventoryItemResult.StorageFailure, result)
        }
    }

    @Test
    fun productAndStockUseSameHousehold() {
        runTest {
            val res = useCase("Name", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertEquals(res.inventoryItem.product.householdId, res.inventoryItem.stockItem.householdId)
            assertEquals(householdProvider.householdId, res.inventoryItem.product.householdId)
        }
    }

    @Test
    fun stockUsesGeneratedProductId() {
        runTest {
            val res = useCase("Name", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertEquals(res.inventoryItem.product.id, res.inventoryItem.stockItem.productId)
        }
    }

    @Test
    fun productIdIsNonBlank() {
        runTest {
            val res = useCase("Name", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertTrue(res.inventoryItem.product.id.value.isNotBlank())
        }
    }

    @Test
    fun stockItemIdIsNonBlank() {
        runTest {
            val res = useCase("Name", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertTrue(res.inventoryItem.stockItem.id.value.isNotBlank())
        }
    }

    @Test
    fun consecutiveAddsGenerateDifferentProductIds() {
        runTest {
            val res1 = useCase("Name1", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            val res2 = useCase("Name2", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertNotEquals(res1.inventoryItem.product.id, res2.inventoryItem.product.id)
        }
    }

    @Test
    fun consecutiveAddsGenerateDifferentStockItemIds() {
        runTest {
            val res1 = useCase("Name1", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            val res2 = useCase("Name2", "1", MeasurementUnit.UNIT) as AddExactInventoryItemResult.Added
            assertNotEquals(res1.inventoryItem.stockItem.id, res2.inventoryItem.stockItem.id)
        }
    }

    @Test
    fun repositoryReceivesOriginalSelectedUnit() {
        runTest {
            useCase("Milk", "1", MeasurementUnit.LITER)
            assertEquals(MeasurementUnit.LITER, repository.addedStockItems[0].quantity.unit)
        }
    }

    @Test
    fun addedResultContainsTheCreatedInventoryItem() {
        runTest {
            val res = useCase("Milk", "1", MeasurementUnit.LITER) as AddExactInventoryItemResult.Added
            assertEquals("Milk", res.inventoryItem.product.name.displayValue)
            assertEquals(0, res.inventoryItem.stockItem.quantity.amount.compareTo(BigDecimal.ONE))
            assertEquals(MeasurementUnit.LITER, res.inventoryItem.stockItem.quantity.unit)
        }
    }
}
