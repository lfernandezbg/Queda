package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeInventoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConsumeExactQuantityUseCaseTest {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var useCase: ConsumeExactQuantityUseCase

    @Before
    fun setUp() {
        repository = FakeInventoryRepository()
        useCase = ConsumeExactQuantityUseCase(repository)
    }

    @Test
    fun invoke_delegatesToRepository() =
        runTest {
            val stockItemId = StockItemId.from("s1")
            val toConsume = ExactQuantity.of("1", MeasurementUnit.UNIT)
            repository.setMutationResult(QuantityMutationResult.Success(ExactQuantity.of("1", MeasurementUnit.UNIT)))

            val result = useCase(stockItemId, toConsume)

            assertTrue(result is QuantityMutationResult.Success)
            assertEquals(stockItemId, repository.consumedExactQuantities[0].first)
            assertEquals(toConsume, repository.consumedExactQuantities[0].second)
        }

    @Test
    fun invoke_returnsFailure_whenRepositoryFails() =
        runTest {
            val stockItemId = StockItemId.from("s1")
            val toConsume = ExactQuantity.of("1", MeasurementUnit.UNIT)
            repository.setMutationResult(QuantityMutationResult.Failure(DomainError.StorageFailure))

            val result = useCase(stockItemId, toConsume)

            assertTrue(result is QuantityMutationResult.Failure)
            assertEquals(DomainError.StorageFailure, (result as QuantityMutationResult.Failure).error)
        }
}
