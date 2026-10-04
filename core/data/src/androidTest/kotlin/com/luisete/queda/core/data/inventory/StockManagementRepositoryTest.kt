package com.luisete.queda.core.data.inventory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luisete.queda.core.data.household.HouseholdSyncEngine
import com.luisete.queda.core.database.LocationEntity
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.FindItemByBarcodeResult
import com.luisete.queda.core.domain.inventory.ReceiptParser
import com.luisete.queda.core.domain.inventory.ReceiptResult
import com.luisete.queda.core.domain.inventory.SelectedStock
import com.luisete.queda.core.domain.inventory.StockWriteResult
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.inventory.FoodType
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class StockManagementRepositoryTest {
    private lateinit var db: QuedaDatabase
    private lateinit var firebase: FirebaseApp
    private lateinit var repository: OfflineStockManagementRepository
    private lateinit var locationsRepository: OfflineLocationsRepository
    private lateinit var receipts: OfflineReceiptRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QuedaDatabase::class.java).build()
        firebase =
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:123:android:test").setProjectId("demo-queda-validation")
                    .setApiKey("validation-fixture").build(),
                UUID.randomUUID().toString(),
            )
        val sync =
            HouseholdSyncEngine(
                FirebaseFirestore.getInstance(firebase),
                FirebaseAuth.getInstance(firebase),
                db,
                db.syncDao(),
            )
        locationsRepository =
            OfflineLocationsRepository(
                db,
                CurrentHouseholdIdProvider { HouseholdId.from("home") },
                sync,
            )
        repository = OfflineStockManagementRepository(db, CurrentHouseholdIdProvider { HouseholdId.from("home") }, sync)
        receipts = OfflineReceiptRepository(db, CurrentHouseholdIdProvider { HouseholdId.from("home") }, sync)
    }

    @After
    fun cleanup() {
        db.close()
        firebase.delete()
    }

    @Test
    fun oneStaleItemRollsBackTheWholeConsumptionAndOutbox() =
        runTest {
            seed("a", "2")
            seed("b", "3")
            val result = repository.consumeSelection(listOf(selected("a", "2"), selected("b", "2")))
            assertEquals(StockWriteResult.STALE, result)
            assertEquals("2", db.inventoryDao().getStockItemById("a")?.quantityAmount)
            assertEquals("3", db.inventoryDao().getStockItemById("b")?.quantityAmount)
            assertTrue(db.syncDao().pending("home").isEmpty())
        }

    @Test
    fun successfulBatchConsumesAllAndUsesOneSyncBatch() =
        runTest {
            seed("a", "2")
            seed("b", "3")
            assertEquals(
                StockWriteResult.SAVED,
                repository.consumeSelection(
                    listOf(
                        selected(
                            "a",
                            "2",
                        ),
                        selected(
                            "b",
                            "3",
                        ),
                    ),
                ),
            )
            assertEquals("0", db.inventoryDao().getStockItemById("a")?.quantityAmount)
            assertEquals("0", db.inventoryDao().getStockItemById("b")?.quantityAmount)
            val operations = db.syncDao().pending("home")
            assertEquals(2, operations.size)
            assertTrue(operations.first().batchId.isNotBlank())
            assertEquals(1, operations.map { it.batchId }.distinct().size)
        }

    @Test
    fun partialConsumptionUsesEachRequestedAmountAndRejectsAnInvalidGroupAtomically() =
        runTest {
            seed("a", "3")
            seed("b", "2")
            val wanted =
                listOf(
                    SelectedStock("a", ExactQuantity.of("3", MeasurementUnit.UNIT), ExactQuantity.of("1", MeasurementUnit.UNIT)),
                    SelectedStock("b", ExactQuantity.of("2", MeasurementUnit.UNIT), ExactQuantity.of("3", MeasurementUnit.UNIT)),
                )
            assertEquals(StockWriteResult.INVALID, repository.consumeSelection(wanted))
            assertEquals("3", db.inventoryDao().getStockItemById("a")?.quantityAmount)
            assertEquals("2", db.inventoryDao().getStockItemById("b")?.quantityAmount)
            assertTrue(db.syncDao().pending("home").isEmpty())

            assertEquals(
                StockWriteResult.SAVED,
                repository.consumeSelection(
                    wanted.mapIndexed { index, item ->
                        if (index == 1) item.copy(toConsume = ExactQuantity.of("2", MeasurementUnit.UNIT)) else item
                    },
                ),
            )
            assertEquals("2", db.inventoryDao().getStockItemById("a")?.quantityAmount)
            assertEquals("0", db.inventoryDao().getStockItemById("b")?.quantityAmount)
            assertEquals(2, db.syncDao().pending("home").size)
        }

    @Test
    fun archivedLocationsKeepExistingFoodAndCannotReceiveNewFood() =
        runTest {
            locationsRepository.saveLocation(null, "Nevera", false)
            val location = locationsRepository.observeLocations().first().single()
            seed("a", "2")
            assertEquals(StockWriteResult.SAVED, repository.moveSelection(listOf(selected("a", "2")), location.id))
            locationsRepository.saveLocation(location.id, "Nevera", true)
            assertEquals(location.id, db.inventoryDao().getStockItemById("a")?.locationId)
            seed("b", "3")
            assertEquals(StockWriteResult.INVALID, repository.moveSelection(listOf(selected("b", "3")), location.id))
            assertEquals(null, db.inventoryDao().getStockItemById("b")?.locationId)
            locationsRepository.saveLocation(location.id, "Nevera", false)
            assertEquals(StockWriteResult.SAVED, repository.moveSelection(listOf(selected("b", "3")), location.id))
        }

    @Test
    fun preparedMealsReuseCanonicalProductButKeepIndependentLots() =
        runTest {
            val details = StockDetails(foodType = FoodType.PREPARED)
            repository.addLot("Lentejas", ExactQuantity.of("2", MeasurementUnit.UNIT), details)
            repository.addLot("Lentejas", ExactQuantity.of("1", MeasurementUnit.UNIT), details)
            assertEquals(1, db.syncDao().products("home").size)
            assertEquals(2, db.syncDao().stockItems("home").size)
            assertEquals(listOf("1", "2"), db.syncDao().stockItems("home").map { it.quantityAmount }.sortedBy { it })
        }

    @Test
    fun barcodeLookupReturnsEveryStockLotForTheSameProduct() =
        runTest {
            val barcode = "4006381333931"
            db.inventoryDao().insertProduct(ProductEntity("shared", "home", "Yogur", "yogur", barcode))
            db.inventoryDao().insertStockItem(StockItemEntity("old", "home", "shared", "EXACT", "2", "UNIT", null))
            db.inventoryDao().insertStockItem(StockItemEntity("new", "home", "shared", "EXACT", "1", "UNIT", null))
            val lookup = (Barcode.create(barcode) as BarcodeCreationResult.Success).barcode
            val inventory =
                OfflineInventoryRepository(
                    db,
                    db.inventoryDao(),
                    db.syncDao(),
                    CurrentHouseholdIdProvider { HouseholdId.from("home") },
                )
            val found = inventory.findItemByBarcode(lookup) as FindItemByBarcodeResult.Found
            assertEquals(setOf("old", "new"), found.candidates.map { it.stockItem.id.value }.toSet())
        }

    @Test
    fun renameUpdatesCanonicalProductAndQueuesAllItsLotsInOneAtomicGroup() =
        runTest {
            db.inventoryDao().insertProduct(ProductEntity("shared", "home", "Pan", "pan", null))
            db.inventoryDao().insertStockItem(StockItemEntity("old", "home", "shared", "EXACT", "2", "UNIT", null))
            db.inventoryDao().insertStockItem(StockItemEntity("new", "home", "shared", "EXACT", "1", "UNIT", null))
            db.inventoryDao().insertProduct(ProductEntity("other", "home", "Leche", "leche", null))
            assertEquals(
                StockWriteResult.DUPLICATE,
                repository.editItem("old", StockDetails(), StockDetails(), "Pan", "Leche"),
            )
            assertEquals("Pan", db.syncDao().product("shared")?.displayName)
            assertTrue(db.syncDao().pending("home").isEmpty())

            assertEquals(
                StockWriteResult.SAVED,
                repository.editItem("old", StockDetails(), StockDetails(label = "Bolsa grande"), "Pan", "Pan integral"),
            )
            assertEquals("Pan integral", db.syncDao().product("shared")?.displayName)
            assertEquals("pan integral", db.syncDao().product("shared")?.normalizedName)
            assertEquals("Bolsa grande", db.managementDao().stock("home", "old")?.label)
            val operations = db.syncDao().pending("home")
            assertEquals(2, operations.size)
            assertTrue(operations.all { it.action == "RENAME" })
            assertEquals(1, operations.map { it.batchId }.distinct().size)
        }

    @Test
    fun householdScopePreventsConsumingAnotherHomesItem() =
        runTest {
            db.inventoryDao().insertProduct(ProductEntity("other", "other-home", "Pan", "pan", null))
            db.inventoryDao().insertStockItem(
                StockItemEntity(
                    "foreign",
                    "other-home",
                    "other",
                    "EXACT",
                    "1",
                    "UNIT",
                    null,
                ),
            )
            assertEquals(StockWriteResult.STALE, repository.consumeSelection(listOf(selected("foreign", "1"))))
            assertEquals("1", db.inventoryDao().getStockItemById("foreign")?.quantityAmount)
            assertTrue(db.syncDao().pending("home").isEmpty())
        }

    @Test
    fun receiptImportIsAtomicAndIdempotentAndPurgesRawText() =
        runTest {
            val parsed = ReceiptParser.parse("PAN 0,90\nLECHE 1,20")
            val valid = parsed.copy(lines = parsed.lines.map { it.copy(selected = true, reviewed = true) })
            assertEquals(ReceiptResult.SAVED, receipts.save(valid))
            val invalid =
                valid.copy(
                    lines =
                        valid.lines.mapIndexed {
                                i,
                                line,
                            ->
                            if (i == 1) line.copy(quantity = "0") else line
                        },
                )
            assertEquals(ReceiptResult.INVALID, receipts.import(invalid))
            assertTrue(db.syncDao().stockItems("home").isEmpty())
            assertTrue(db.syncDao().pending("home").isEmpty())
            assertEquals(ReceiptResult.IMPORTED, receipts.import(valid))
            assertEquals(2, db.syncDao().stockItems("home").size)
            assertEquals(ReceiptResult.DUPLICATE, receipts.import(valid))
            assertEquals(2, db.syncDao().stockItems("home").size)
            assertEquals("[]", db.managementDao().receipt("home", valid.fingerprint)?.payload)
        }

    @Test
    fun savedReceiptEditsAreRestoredFromRoom() =
        runTest {
            val parsed = ReceiptParser.parse("PAN 0,90")
            val edited =
                parsed.copy(
                    lines =
                        parsed.lines.map {
                            it.copy(
                                name = "Pan integral",
                                quantity = "2",
                                reviewed = true,
                            )
                        },
                )
            receipts.save(edited)
            assertEquals(edited, receipts.observeDraft().first())
            receipts.discard(edited.id)
            assertEquals(null, receipts.observeDraft().first())
        }

    @Test
    fun locationConflictResolutionRetainsLocalEditsOrAcceptsRemoteRevision() =
        runTest {
            val remote = """{"name":"Nevera compartida","normalizedName":"nevera compartida","archived":true,"revision":4}"""
            db.managementDao().saveLocation(
                LocationEntity(
                    "local",
                    "home",
                    "Mi nevera",
                    "mi nevera",
                    false,
                    revision = 2,
                    syncPending = true,
                    conflictPayload = remote,
                ),
            )
            assertTrue(locationsRepository.observeLocations().first().single().conflicted)
            assertEquals(StockWriteResult.SAVED, locationsRepository.resolveConflict("local", true))
            val retained = requireNotNull(db.managementDao().location("home", "local"))
            assertEquals("Mi nevera", retained.name)
            assertEquals(4L, retained.revision)
            assertTrue(retained.syncPending)
            assertEquals(null, retained.conflictPayload)
            db.managementDao().saveLocation(retained.copy(conflictPayload = remote))
            assertEquals(StockWriteResult.SAVED, locationsRepository.resolveConflict("local", false))
            val accepted = requireNotNull(db.managementDao().location("home", "local"))
            assertEquals("Nevera compartida", accepted.name)
            assertTrue(accepted.archived)
            assertEquals(false, accepted.syncPending)
            assertEquals(null, accepted.conflictPayload)
        }

    private suspend fun seed(
        id: String,
        amount: String,
    ) {
        db.inventoryDao().insertProduct(ProductEntity("p-$id", "home", "Food $id", "food $id", null))
        db.inventoryDao().insertStockItem(StockItemEntity(id, "home", "p-$id", "EXACT", amount, "UNIT", null))
    }

    private fun selected(
        id: String,
        amount: String,
    ) = SelectedStock(id, ExactQuantity.of(amount, MeasurementUnit.UNIT))
}
