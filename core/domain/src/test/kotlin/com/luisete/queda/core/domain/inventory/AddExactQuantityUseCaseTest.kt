package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeInventoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddExactQuantityUseCaseTest {
    @Test fun forwardsPositiveExactDeltaToRepository() =
        runTest {
            val repository = FakeInventoryRepository()
            val itemId = StockItemId.from("stock-1")
            val delta = ExactQuantity.of("1.25", MeasurementUnit.KILOGRAM)
            val expectedQuantity = ExactQuantity.of("3.25", MeasurementUnit.KILOGRAM)
            repository.setMutationResult(QuantityMutationResult.Success(expectedQuantity))

            val result = AddExactQuantityUseCase(repository)(itemId, delta)

            assertEquals(listOf(itemId to delta), repository.increasedExactQuantities)
            assertEquals(QuantityMutationResult.Success(expectedQuantity), result)
        }
}
