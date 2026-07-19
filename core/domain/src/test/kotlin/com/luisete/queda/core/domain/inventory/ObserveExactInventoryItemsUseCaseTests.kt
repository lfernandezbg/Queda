package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.testing.FakeCurrentHouseholdIdProvider
import com.luisete.queda.core.testing.FakeInventoryRepository
import com.luisete.queda.core.testing.InventoryTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveExactInventoryItemsUseCaseTests {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var householdProvider: FakeCurrentHouseholdIdProvider
    private lateinit var useCase: ObserveExactInventoryItemsUseCase

    @Before
    fun setUp() {
        repository = FakeInventoryRepository()
        householdProvider = FakeCurrentHouseholdIdProvider()
        useCase = ObserveExactInventoryItemsUseCase(repository, householdProvider)
    }

    @Test
    fun currentHouseholdIsPassedToRepository() {
        runTest {
            repository.emit(emptyList())
            useCase().first()

            assertEquals(1, repository.observeSubscriptionsCount)
            assertEquals(1, repository.observedHouseholdIds.size)
            assertEquals(householdProvider.householdId, repository.observedHouseholdIds[0])
        }
    }

    @Test
    fun emptyListIsEmitted() {
        runTest {
            repository.emit(emptyList())
            val result = useCase().first()
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun repositoryItemsAreEmittedUnmodified() {
        runTest {
            val item = InventoryTestData.createInventoryItem()
            repository.emit(listOf(item))
            val result = useCase().first()
            assertEquals(1, result.size)
            assertEquals(item, result[0])
        }
    }

    @Test(expected = RuntimeException::class)
    fun repositoryFlowFailureIsNotSilentlyReplaced() {
        runTest {
            repository.setFlowError(RuntimeException("DB Error"))
            useCase().first()
        }
    }
}
