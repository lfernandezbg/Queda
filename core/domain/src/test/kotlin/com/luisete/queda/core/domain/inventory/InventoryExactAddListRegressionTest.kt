package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeCurrentHouseholdIdProvider
import com.luisete.queda.core.testing.FakeInventoryRepository
import com.luisete.queda.core.testing.InventoryTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class InventoryExactAddListRegressionTest {
    @Test
    fun phase11MixedMassFallbackStillWorks() {
        val q1 = ExactQuantity.of(BigDecimal.ONE, MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of(BigDecimal("0.5"), MeasurementUnit.GRAM)
        val result = QuantityOperations.add(q1, q2).successValue()
        assertEquals(MeasurementUnit.GRAM, result.unit)
        assertEquals(0, result.amount.compareTo(BigDecimal("1000.5")))
    }

    @Test
    fun phase11MixedVolumeFallbackStillWorks() {
        val q1 = ExactQuantity.of(BigDecimal.ONE, MeasurementUnit.LITER)
        val q2 = ExactQuantity.of(BigDecimal("0.5"), MeasurementUnit.MILLILITER)
        val result = QuantityOperations.add(q1, q2).successValue()
        assertEquals(MeasurementUnit.MILLILITER, result.unit)
        assertEquals(0, result.amount.compareTo(BigDecimal("1000.5")))
    }

    @Test
    fun commaDecimalDoesNotReachBigDecimalUnconverted() {
        runTest {
            val res = ExactQuantityInputParser.parse("1,25", MeasurementUnit.UNIT) as ExactQuantityInputResult.Success
            assertEquals(0, res.quantity.amount.compareTo(BigDecimal("1.25")))
        }
    }

    @Test
    fun trailingZerosDoNotCreateFalsePrecisionError() {
        runTest {
            val res = ExactQuantityInputParser.parse("1.2000", MeasurementUnit.UNIT) as ExactQuantityInputResult.Success
            assertEquals(0, res.quantity.amount.compareTo(BigDecimal("1.2")))
        }
    }

    @Test
    fun caseVariantDuplicateCannotBeInserted() {
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            useCase("Milk", "1", MeasurementUnit.LITER)

            repository.setAddResult(AddExactItemRepositoryResult.DuplicateProductName)
            val res = useCase("MILK", "1", MeasurementUnit.LITER)
            assertEquals(AddExactInventoryItemResult.DuplicateProductName, res)
        }
    }

    @Test
    fun spacingVariantDuplicateCannotBeInserted() {
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            useCase("Leche", "1", MeasurementUnit.LITER)

            repository.setAddResult(AddExactItemRepositoryResult.DuplicateProductName)
            val res = useCase("  Leche  ", "1", MeasurementUnit.LITER)
            assertEquals(AddExactInventoryItemResult.DuplicateProductName, res)
        }
    }

    @Test
    fun invalidNameWithValidQuantityCannotCreateSyntheticProduct() {
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            useCase("", "1", MeasurementUnit.UNIT)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun zeroQuantityCannotReachRepository() {
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            val res = useCase("Milk", "0", MeasurementUnit.LITER)
            assertTrue(res is AddExactInventoryItemResult.InvalidInput)
            assertEquals(0, repository.addCallsCount)
        }
    }

    @Test
    fun inventoryFlowDoesNotLosePersistedItemAfterResubscription() {
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = ObserveExactInventoryItemsUseCase(repository, FakeCurrentHouseholdIdProvider())
            val item = InventoryTestData.createInventoryItem()
            repository.emit(listOf(item))

            assertEquals(1, useCase().first().size)
            assertEquals(1, useCase().first().size)
        }
    }

    @Test
    fun blankFormCannotCreateAnyInventoryItem() {
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            useCase("", "", MeasurementUnit.UNIT)
            assertEquals(0, repository.addCallsCount)
        }
    }

    private fun <T> com.luisete.queda.core.domain.result.DomainResult<T>.successValue(): T =
        (this as com.luisete.queda.core.domain.result.Success<T>).value
}
