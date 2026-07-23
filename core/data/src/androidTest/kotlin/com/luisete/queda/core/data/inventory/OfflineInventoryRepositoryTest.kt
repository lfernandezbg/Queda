package com.luisete.queda.core.data.inventory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luisete.queda.core.database.InventoryDao
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.domain.inventory.FindItemByBarcodeResult
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import com.luisete.queda.core.testing.InventoryTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineInventoryRepositoryTest {
    private lateinit var db: QuedaDatabase
    private lateinit var dao: InventoryDao
    private lateinit var repository: OfflineInventoryRepository
    private val householdId = InventoryTestData.householdId

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, QuedaDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.inventoryDao()
        repository = OfflineInventoryRepository(db, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addedDatabaseResultMapsToAdded() =
        runTest {
            val item = InventoryTestData.createInventoryItem()
            val result = repository.addExactInventoryItem(item.product, item.stockItem)
            assertEquals(AddExactItemRepositoryResult.Added, result)
        }

    @Test
    fun duplicateDatabaseResultMapsToDuplicateProductName() =
        runTest {
            val item = InventoryTestData.createInventoryItem()
            repository.addExactInventoryItem(item.product, item.stockItem)
            val result = repository.addExactInventoryItem(item.product, item.stockItem)
            assertEquals(AddExactItemRepositoryResult.DuplicateProductName, result)
        }

    @Test
    fun allFiveUnitsPreserveAmountAndUnit() =
        runTest {
            val units =
                listOf(
                    MeasurementUnit.UNIT,
                    MeasurementUnit.GRAM,
                    MeasurementUnit.KILOGRAM,
                    MeasurementUnit.MILLILITER,
                    MeasurementUnit.LITER,
                )
            units.forEach { unit ->
                val product = InventoryTestData.createProduct(id = "p-${unit.name}", name = "Product ${unit.name}")
                val stockItem =
                    InventoryTestData.createStockItem(
                        id = "s-${unit.name}",
                        productId = product.id.value,
                        quantity = ExactQuantity.of("1.234", unit),
                    )

                repository.addExactInventoryItem(product, stockItem)
            }

            val res = repository.observeExactInventoryItems(householdId).first()
            assertEquals(5, res.size)

            units.forEach { unit ->
                val found = res.find { it.stockItem.id.value == "s-${unit.name}" }
                assertTrue("Item with unit ${unit.name} should be found", found != null)
                val quantity = found?.stockItem?.quantity as ExactQuantity
                assertEquals("1.234", quantity.amount.toPlainString())
                assertEquals(unit, quantity.unit)
            }
        }

    @Test
    fun observedItemsAreSortedByNormalizedName() =
        runTest {
            val p1 = InventoryTestData.createProduct(id = "p1", name = "Zebra")
            val s1 = InventoryTestData.createStockItem(id = "s1", productId = p1.id.value)
            val p2 = InventoryTestData.createProduct(id = "p2", name = "Apple")
            val s2 = InventoryTestData.createStockItem(id = "s2", productId = p2.id.value)

            repository.addExactInventoryItem(p1, s1)
            repository.addExactInventoryItem(p2, s2)

            val result = repository.observeExactInventoryItems(householdId).first()
            assertEquals(2, result.size)
            assertEquals("Apple", result[0].product.name.displayValue)
            assertEquals("Zebra", result[1].product.name.displayValue)
        }

    @Test
    fun consumeExactQuantityPersistsResult() =
        runTest {
            val sid = "s1"
            dao.insertProduct(ProductEntity("p1", householdId.value, "Milk", "milk"))
            dao.insertStockItem(StockItemEntity(sid, householdId.value, "p1", "EXACT", "10", "UNIT", null))

            val result =
                repository.consumeExactQuantity(
                    StockItemId.from(sid),
                    ExactQuantity.of("3", MeasurementUnit.UNIT),
                )

            val success = result as QuantityMutationResult.Success
            val quantity = success.newQuantity as ExactQuantity
            assertEquals("7", quantity.amount.toPlainString())

            val persisted = dao.getStockItemById(sid)
            assertEquals("7", persisted?.quantityAmount)
        }

    @Test
    fun correctExactQuantityPersistsResult() =
        runTest {
            val sid = "s1"
            dao.insertProduct(ProductEntity("p1", householdId.value, "Milk", "milk"))
            dao.insertStockItem(StockItemEntity(sid, householdId.value, "p1", "EXACT", "10", "UNIT", null))

            val result =
                repository.correctExactQuantity(
                    StockItemId.from(sid),
                    ExactQuantity.of("5", MeasurementUnit.UNIT),
                )

            val success = result as QuantityMutationResult.Success
            val quantity = success.newQuantity as ExactQuantity
            assertEquals("5", quantity.amount.toPlainString())

            val persisted = dao.getStockItemById(sid)
            assertEquals("5", persisted?.quantityAmount)
        }

    @Test
    fun mutationWithMissingProductReturnsProductNotFound() =
        runTest {
            val sid = StockItemId.from("missing")
            val result = repository.consumeExactQuantity(sid, ExactQuantity.of("1", MeasurementUnit.UNIT))

            val failure = result as QuantityMutationResult.Failure
            assertEquals(DomainError.ProductNotFound, failure.error)
        }

    @Test
    fun rejectedMutationPerformsNoWrite() =
        runTest {
            val sid = "s1"
            dao.insertProduct(ProductEntity("p1", householdId.value, "Milk", "milk"))
            dao.insertStockItem(StockItemEntity(sid, householdId.value, "p1", "EXACT", "10", "UNIT", null))

            // Consume more than available
            val result =
                repository.consumeExactQuantity(
                    StockItemId.from(sid),
                    ExactQuantity.of("11", MeasurementUnit.UNIT),
                )

            assertTrue(result is QuantityMutationResult.Failure)
            assertEquals(DomainError.AmountMustBeLowerThanCurrent, (result as QuantityMutationResult.Failure).error)

            val persisted = dao.getStockItemById(sid)
            assertEquals("10", persisted?.quantityAmount)
        }

    @Test
    fun unchangedCorrectionPerformsNoWrite() =
        runTest {
            val sid = "s1"
            dao.insertProduct(ProductEntity("p1", householdId.value, "Milk", "milk"))
            dao.insertStockItem(StockItemEntity(sid, householdId.value, "p1", "EXACT", "10", "UNIT", null))

            val result =
                repository.correctExactQuantity(
                    StockItemId.from(sid),
                    ExactQuantity.of("10", MeasurementUnit.UNIT),
                )

            assertTrue(result is QuantityMutationResult.Failure)
            assertEquals(DomainError.UnchangedQuantity, (result as QuantityMutationResult.Failure).error)

            val persisted = dao.getStockItemById(sid)
            assertEquals("10", persisted?.quantityAmount)
        }

    @Test
    fun findByBarcodeReturnsItem() =
        runTest {
            val barcodeStr = "4006381333931"
            val barcode = (Barcode.create(barcodeStr) as BarcodeCreationResult.Success).barcode
            val product = InventoryTestData.createProduct(barcode = barcodeStr)
            val stockItem = InventoryTestData.createStockItem(productId = product.id.value)
            repository.addExactInventoryItem(product, stockItem)

            val result = repository.findItemByBarcode(barcode)
            assertTrue(result is FindItemByBarcodeResult.Found)
            assertEquals(product.id, (result as FindItemByBarcodeResult.Found).item.product.id)
        }

    @Test
    fun findByMissingBarcodeReturnsNotFound() =
        runTest {
            val barcode = (Barcode.create("73513537") as BarcodeCreationResult.Success).barcode
            val result = repository.findItemByBarcode(barcode)
            assertEquals(FindItemByBarcodeResult.NotFound, result)
        }

    @Test
    fun duplicateBarcodeReturnsDuplicateBarcodeResult() =
        runTest {
            val barcode = "4006381333931"
            val p1 = InventoryTestData.createProduct(id = "p1", name = "P1", barcode = barcode)
            val s1 = InventoryTestData.createStockItem(id = "s1", productId = "p1")
            repository.addExactInventoryItem(p1, s1)

            val p2 = InventoryTestData.createProduct(id = "p2", name = "P2", barcode = barcode)
            val s2 = InventoryTestData.createStockItem(id = "s2", productId = "p2")

            val result = repository.addExactInventoryItem(p2, s2)
            assertEquals(AddExactItemRepositoryResult.DuplicateBarcode, result)
        }

    @Test
    fun setPresencePersistsResult() =
        runTest {
            val sid = "s1"
            dao.insertProduct(ProductEntity("p1", householdId.value, "Salt", "salt"))
            dao.insertStockItem(StockItemEntity(sid, householdId.value, "p1", "PRESENCE", null, null, true))

            val result = repository.setPresence(StockItemId.from(sid), false)

            assertTrue(result is QuantityMutationResult.Success)
            assertEquals(false, (result as QuantityMutationResult.Success).newQuantity.let { (it as PresenceQuantity).isPresent })

            val persisted = dao.getStockItemById(sid)
            assertEquals(false, persisted?.isPresent)
        }

    @Test
    fun setPresenceWithIncompatibleModeReturnsIncompatibleMode() =
        runTest {
            val sid = "s1"
            dao.insertProduct(ProductEntity("p1", householdId.value, "Milk", "milk"))
            dao.insertStockItem(StockItemEntity(sid, householdId.value, "p1", "EXACT", "10", "UNIT", null))

            val result = repository.setPresence(StockItemId.from(sid), false)

            val failure = result as QuantityMutationResult.Failure
            assertEquals(DomainError.IncompatibleMode, failure.error)
        }

    private fun <T> Any.getOrThrow(): T =
        when (this) {
            is com.luisete.queda.core.model.barcode.BarcodeCreationResult.Success -> barcode as T
            else -> throw IllegalArgumentException("Result is not success")
        }
}
