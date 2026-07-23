package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.PresenceQuantity
import com.luisete.queda.core.testing.FakeInventoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SetPresenceUseCaseTests {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var useCase: SetPresenceUseCase

    @Before
    fun setUp() {
        repository = FakeInventoryRepository()
        useCase = SetPresenceUseCase(repository)
    }

    @Test
    fun setPresenceCallsRepository() =
        runTest {
            val id = StockItemId.newId()
            repository.setMutationResult(QuantityMutationResult.Success(PresenceQuantity(true)))

            val result = useCase(id, true)

            assertEquals(QuantityMutationResult.Success(PresenceQuantity(true)), result)
            assertEquals(1, repository.setPresenceCalls.size)
            assertEquals(id to true, repository.setPresenceCalls[0])
        }

    @Test
    fun setPresenceFalseCallsRepository() =
        runTest {
            val id = StockItemId.newId()
            repository.setMutationResult(QuantityMutationResult.Success(PresenceQuantity(false)))

            val result = useCase(id, false)

            assertEquals(QuantityMutationResult.Success(PresenceQuantity(false)), result)
            assertEquals(1, repository.setPresenceCalls.size)
            assertEquals(id to false, repository.setPresenceCalls[0])
        }
}
