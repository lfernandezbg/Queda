package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.InventoryItemProjection
import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.testing.InventoryTestData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OfflineInventoryRepositoryTest {
    private lateinit var dao: FakeInventoryDao
    private lateinit var repository: OfflineInventoryRepository
    private val householdId = HouseholdId.from("h1")

    @Before
    fun setUp() {
        dao = FakeInventoryDao()
        repository = OfflineInventoryRepository(dao)
    }

    @Test
    fun addedDatabaseResultMapsToAdded() {
        runTest {
            val item = InventoryTestData.createInventoryItem()
            val result = repository.addExactInventoryItem(item.product, item.stockItem)
            assertEquals(AddExactItemRepositoryResult.Added, result)
        }
    }

    @Test
    fun duplicateDatabaseResultMapsToDuplicateProductName() {
        runTest {
            val item = InventoryTestData.createInventoryItem()
            repository.addExactInventoryItem(item.product, item.stockItem)
            val result = repository.addExactInventoryItem(item.product, item.stockItem)
            assertEquals(AddExactItemRepositoryResult.DuplicateProductName, result)
        }
    }

    @Test
    fun storageExceptionMapsToStorageFailure() {
        runTest {
            val item = InventoryTestData.createInventoryItem()
            dao.throwOnInsert = IOException("Disk full")
            val result = repository.addExactInventoryItem(item.product, item.stockItem)
            assertEquals(AddExactItemRepositoryResult.StorageFailure, result)
        }
    }

    @Test(expected = CancellationException::class)
    fun cancellationExceptionIsRethrown() {
        runTest {
            val item = InventoryTestData.createInventoryItem()
            dao.throwOnInsert = CancellationException()
            repository.addExactInventoryItem(item.product, item.stockItem)
        }
    }

    @Test
    fun projectionMapsToInventoryItem() {
        runTest {
            val p =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "Milk",
                    productNormalizedName = "milk",
                    stockItemId = "s1",
                    stockHouseholdId = "h1",
                    stockProductId = "p1",
                    quantityAmount = "1",
                    quantityUnit = "LITER",
                )
            dao.emit(listOf(p))

            val result = repository.observeExactInventoryItems(householdId).first()
            assertEquals(1, result.size)
            assertEquals("Milk", result[0].product.name.displayValue)
        }
    }

    @Test
    fun allFiveUnitsPreserveAmountAndUnit() {
        runTest {
            val units = listOf("UNIT", "GRAM", "KILOGRAM", "MILLILITER", "LITER")
            units.forEach { unit ->
                val p =
                    InventoryItemProjection(
                        productId = "p-$unit",
                        productHouseholdId = "h1",
                        productDisplayName = "Name",
                        productNormalizedName = "name",
                        stockItemId = "s-$unit",
                        stockHouseholdId = "h1",
                        stockProductId = "p-$unit",
                        quantityAmount = "1.234",
                        quantityUnit = unit,
                    )
                dao.emit(listOf(p))
                val res = repository.observeExactInventoryItems(householdId).first()
                assertEquals("1.234", res[0].stockItem.quantity.amount.toPlainString())
                assertEquals(unit, res[0].stockItem.quantity.unit.name)
            }
        }
    }

    @Test
    fun householdIdIsPassedToDao() {
        runTest {
            dao.emit(emptyList())
            repository.observeExactInventoryItems(householdId).first()
            assertEquals(1, dao.observedHouseholdIds.size)
            assertEquals("h1", dao.observedHouseholdIds[0])
        }
    }

    @Test
    fun duplicateDoesNotLeavePartialDomainState() {
        runTest {
            val item = InventoryTestData.createInventoryItem()
            repository.addExactInventoryItem(item.product, item.stockItem)
            repository.addExactInventoryItem(item.product, item.stockItem)
            assertEquals(1, dao.products.size)
        }
    }

    @Test
    fun observedItemsRemainInDaoOrder() {
        runTest {
            val p1 =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "A",
                    productNormalizedName = "a",
                    stockItemId = "s1",
                    stockHouseholdId = "h1",
                    stockProductId = "p1",
                    quantityAmount = "1",
                    quantityUnit = "UNIT",
                )
            val p2 =
                InventoryItemProjection(
                    productId = "p2",
                    productHouseholdId = "h1",
                    productDisplayName = "B",
                    productNormalizedName = "b",
                    stockItemId = "s2",
                    stockHouseholdId = "h1",
                    stockProductId = "p2",
                    quantityAmount = "1",
                    quantityUnit = "UNIT",
                )
            dao.emit(listOf(p1, p2))

            val result = repository.observeExactInventoryItems(householdId).first()
            assertEquals("A", result[0].product.name.displayValue)
            assertEquals("B", result[1].product.name.displayValue)
        }
    }

    @Test
    fun corruptedNormalizedNameProducesError() {
        runTest {
            val p =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "Milk",
                    productNormalizedName = "WRONG",
                    stockItemId = "s1",
                    stockHouseholdId = "h1",
                    stockProductId = "p1",
                    quantityAmount = "1",
                    quantityUnit = "LITER",
                )
            dao.emit(listOf(p))

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.observeExactInventoryItems(householdId).first()
                }
            }
        }
    }

    @Test
    fun nonCanonicalDisplayNameProducesError() {
        runTest {
            val p =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "  Milk  ",
                    productNormalizedName = "milk",
                    stockItemId = "s1",
                    stockHouseholdId = "h1",
                    stockProductId = "p1",
                    quantityAmount = "1",
                    quantityUnit = "LITER",
                )
            dao.emit(listOf(p))

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.observeExactInventoryItems(householdId).first()
                }
            }
        }
    }

    @Test
    fun nonCanonicalAmountProducesError() {
        runTest {
            val p =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "Milk",
                    productNormalizedName = "milk",
                    stockItemId = "s1",
                    stockHouseholdId = "h1",
                    stockProductId = "p1",
                    quantityAmount = "1.0",
                    quantityUnit = "LITER",
                )
            dao.emit(listOf(p))

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.observeExactInventoryItems(householdId).first()
                }
            }
        }
    }

    @Test
    fun mismatchedStockHouseholdProducesError() {
        runTest {
            val p =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "Milk",
                    productNormalizedName = "milk",
                    stockItemId = "s1",
                    stockHouseholdId = "h2",
                    stockProductId = "p1",
                    quantityAmount = "1",
                    quantityUnit = "LITER",
                )
            dao.emit(listOf(p))

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.observeExactInventoryItems(householdId).first()
                }
            }
        }
    }

    @Test
    fun mismatchedStockProductProducesError() {
        runTest {
            val p =
                InventoryItemProjection(
                    productId = "p1",
                    productHouseholdId = "h1",
                    productDisplayName = "Milk",
                    productNormalizedName = "milk",
                    stockItemId = "s1",
                    stockHouseholdId = "h1",
                    stockProductId = "p2",
                    quantityAmount = "1",
                    quantityUnit = "LITER",
                )
            dao.emit(listOf(p))

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.observeExactInventoryItems(householdId).first()
                }
            }
        }
    }
}
