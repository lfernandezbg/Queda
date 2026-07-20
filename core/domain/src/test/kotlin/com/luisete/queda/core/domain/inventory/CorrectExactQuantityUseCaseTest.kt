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

class CorrectExactQuantityUseCaseTest {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var useCase: CorrectExactQuantityUseCase

    @Before
    fun setUp() {
        repository = FakeInventoryRepository()
        useCase = CorrectExactQuantityUseCase(repository)
    }

    @Test
    fun invoke_delegatesToRepository() =
        runTest {
            val stockItemId = StockItemId.from("s1")
            val newQuantity = ExactQuantity.of("10", MeasurementUnit.UNIT)
            repository.setMutationResult(QuantityMutationResult.Success(newQuantity))

            val result = useCase(stockItemId, newQuantity)

            assertTrue(result is QuantityMutationResult.Success)
            assertEquals(stockItemId, repository.correctedExactQuantities[0].first)
            assertEquals(newQuantity, repository.correctedExactQuantities[0].second)
        }

    @Test
    fun invoke_returnsFailure_whenRepositoryFails() =
        runTest {
            val stockItemId = StockItemId.from("s1")
            val newQuantity = ExactQuantity.of("10", MeasurementUnit.UNIT)
            repository.setMutationResult(QuantityMutationResult.Failure(DomainError.StorageFailure))

            val result = useCase(stockItemId, newQuantity)

            assertTrue(result is QuantityMutationResult.Failure)
            assertEquals(DomainError.StorageFailure, (result as QuantityMutationResult.Failure).error)
        }
}
