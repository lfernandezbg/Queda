package com.luisete.queda.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryDaoTest {
    private lateinit var db: QuedaDatabase
    private lateinit var dao: InventoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QuedaDatabase::class.java).build()
        dao = db.inventoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun emptyDatabaseEmitsEmptyList() =
        runTest {
            val result = dao.observeExactInventoryItems("h1").first()
            assertTrue(result.isEmpty())
        }

    @Test
    fun insertedProductAndStockAreObserved() =
        runTest {
            val product = ProductEntity("p1", "h1", "Milk", "milk")
            val stock = StockItemEntity("s1", "h1", "p1", "1", "LITER")
            dao.insertProduct(product)
            dao.insertStockItem(stock)

            val result = dao.observeExactInventoryItems("h1").first()
            assertEquals(1, result.size)
            assertEquals("p1", result[0].productId)
            assertEquals("s1", result[0].stockItemId)
        }

    @Test
    fun allFiveUnitsRoundTripWithoutPrecisionLoss() =
        runTest {
            val units = listOf("UNIT", "GRAM", "KILOGRAM", "MILLILITER", "LITER")
            units.forEachIndexed { i, unit ->
                val id = "p$i"
                val sid = "s$i"
                val amount = "1.234"
                dao.insertProduct(ProductEntity(id, "h1", "Item$i", "item$i"))
                dao.insertStockItem(StockItemEntity(sid, "h1", id, amount, unit))
            }

            val result = dao.observeExactInventoryItems("h1").first()
            assertEquals(5, result.size)
            result.forEach {
                assertEquals("1.234", it.quantityAmount)
            }
        }

    @Test
    fun itemsAreOrderedByNormalizedNameThenProductIdThenStockId() =
        runTest {
            // Unique names: Apple, Bread, Zebra
            dao.insertProduct(ProductEntity("p_zebra", "h1", "Zebra", "zebra"))
            dao.insertStockItem(StockItemEntity("s1", "h1", "p_zebra", "1", "UNIT"))

            dao.insertProduct(ProductEntity("p_apple1", "h1", "Apple", "apple"))
            dao.insertStockItem(StockItemEntity("s2", "h1", "p_apple1", "1", "UNIT"))

            dao.insertProduct(ProductEntity("p_bread", "h1", "Bread", "bread"))
            dao.insertStockItem(StockItemEntity("s3", "h1", "p_bread", "1", "UNIT"))

            // Same product name is forbidden by unique index, so I'll use different names
            // But the rule says check order by p.id and s.id.
            // I'll add two stock items for same product (possible in DB)
            dao.insertStockItem(StockItemEntity("s0", "h1", "p_apple1", "2", "UNIT"))

            val result = dao.observeExactInventoryItems("h1").first()
            assertEquals(4, result.size)

            // Apple p_apple1 s0
            assertEquals("apple", result[0].productNormalizedName)
            assertEquals("p_apple1", result[0].productId)
            assertEquals("s0", result[0].stockItemId)

            // Apple p_apple1 s2
            assertEquals("apple", result[1].productNormalizedName)
            assertEquals("p_apple1", result[1].productId)
            assertEquals("s2", result[1].stockItemId)

            // Bread
            assertEquals("bread", result[2].productNormalizedName)

            // Zebra
            assertEquals("zebra", result[3].productNormalizedName)
        }

    @Test
    fun differentHouseholdsAreIsolated() =
        runTest {
            dao.insertProduct(ProductEntity("p1", "h1", "Milk", "milk"))
            dao.insertStockItem(StockItemEntity("s1", "h1", "p1", "1", "UNIT"))
            dao.insertProduct(ProductEntity("p2", "h2", "Bread", "bread"))
            dao.insertStockItem(StockItemEntity("s2", "h2", "p2", "1", "UNIT"))

            assertEquals(1, dao.observeExactInventoryItems("h1").first().size)
            assertEquals(1, dao.observeExactInventoryItems("h2").first().size)
        }

    @Test
    fun duplicateNormalizedNameInSameHouseholdIsRejected() =
        runTest {
            dao.insertProduct(ProductEntity("p1", "h1", "Milk", "milk"))
            dao.insertStockItem(StockItemEntity("s1", "h1", "p1", "1", "UNIT"))

            val duplicate = ProductEntity("p2", "h1", "MILK", "milk")
            val stock = StockItemEntity("s2", "h1", "p2", "1", "UNIT")

            val result = dao.addExactInventoryItem(duplicate, stock)
            assertEquals(AddExactInventoryItemDbResult.DuplicateProductName, result)
        }

    @Test
    fun sameNormalizedNameInDifferentHouseholdsIsAllowed() =
        runTest {
            dao.addExactInventoryItem(
                ProductEntity("p1", "h1", "Milk", "milk"),
                StockItemEntity("s1", "h1", "p1", "1", "UNIT"),
            )
            dao.addExactInventoryItem(
                ProductEntity("p2", "h2", "Milk", "milk"),
                StockItemEntity("s2", "h2", "p2", "1", "UNIT"),
            )
            assertEquals(1, dao.observeExactInventoryItems("h1").first().size)
            assertEquals(1, dao.observeExactInventoryItems("h2").first().size)
        }

    @Test
    fun stockInsertFailureRollsBackProductInsert() =
        runTest {
            // Pre-insert a stock item with ID "s1" to cause duplicate PK failure later
            dao.insertProduct(ProductEntity("p0", "h1", "Other", "other"))
            dao.insertStockItem(StockItemEntity("s1", "h1", "p0", "1", "UNIT"))

            val product = ProductEntity("p1", "h1", "Milk", "milk")
            val duplicateStockId = StockItemEntity("s1", "h1", "p1", "1", "UNIT")

            try {
                dao.addExactInventoryItem(product, duplicateStockId)
            } catch (e: SQLiteConstraintException) {
                // Expected
            }

            // Verify product "p1" was rolled back and only "p0" exists
            val observed = dao.observeExactInventoryItems("h1").first()
            assertEquals(1, observed.size)
            assertEquals("p0", observed[0].productId)
            assertEquals(0, dao.countProductsWithName("h1", "milk"))
        }

    @Test
    fun observerEmitsAfterSuccessfulInsert() =
        runTest {
            val flow = dao.observeExactInventoryItems("h1")
            assertTrue(flow.first().isEmpty())

            dao.addExactInventoryItem(
                ProductEntity("p1", "h1", "Milk", "milk"),
                StockItemEntity("s1", "h1", "p1", "1", "UNIT"),
            )

            assertEquals(1, flow.first().size)
        }

    @Test
    fun mismatchedStockHouseholdIsExcludedFromResults() =
        runTest {
            val product = ProductEntity("p1", "h1", "Milk", "milk")
            // Stock item points to p1 but belongs to h2
            val stock = StockItemEntity("s1", "h2", "p1", "1", "LITER")
            dao.insertProduct(product)
            dao.insertStockItem(stock)

            val h1Result = dao.observeExactInventoryItems("h1").first()
            val h2Result = dao.observeExactInventoryItems("h2").first()
            assertTrue(h1Result.isEmpty())
            assertTrue(h2Result.isEmpty())
        }

    @Test
    fun mismatchedProductIdIsRejectedBeforeWriting() =
        runTest {
            val product = ProductEntity("p1", "h1", "Milk", "milk")
            val stock = StockItemEntity("s1", "h1", "p2", "1", "LITER") // p2 != p1

            try {
                dao.addExactInventoryItem(product, stock)
                org.junit.Assert.fail("Expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }

    @Test
    fun mismatchedHouseholdIsRejectedBeforeWriting() =
        runTest {
            val product = ProductEntity("p1", "h1", "Milk", "milk")
            val stock = StockItemEntity("s1", "h2", "p1", "1", "LITER") // h2 != h1

            try {
                dao.addExactInventoryItem(product, stock)
                org.junit.Assert.fail("Expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
}
