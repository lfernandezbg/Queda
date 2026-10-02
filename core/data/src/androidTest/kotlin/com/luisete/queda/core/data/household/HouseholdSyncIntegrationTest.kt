@file:Suppress("detekt:MaxLineLength", "detekt:LongMethod", "detekt:LargeClass")

package com.luisete.queda.core.data.household

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luisete.queda.core.data.inventory.OfflineInventoryRepository
import com.luisete.queda.core.data.shopping.OfflineShoppingRepository
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.shopping.ShoppingResult
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HouseholdSyncIntegrationTest {
    private lateinit var context: Context
    private lateinit var appA: FirebaseApp
    private lateinit var appB: FirebaseApp
    private lateinit var authA: FirebaseAuth
    private lateinit var authB: FirebaseAuth
    private lateinit var firestoreA: FirebaseFirestore
    private lateinit var firestoreB: FirebaseFirestore

    private lateinit var dbA: QuedaDatabase
    private lateinit var dbB: QuedaDatabase

    private lateinit var syncA: HouseholdSyncEngine
    private lateinit var syncB: HouseholdSyncEngine
    private lateinit var shoppingSyncA: ShoppingSyncEngine
    private lateinit var shoppingSyncB: ShoppingSyncEngine

    private lateinit var managerA: FirebaseHouseholdManager
    private lateinit var managerB: FirebaseHouseholdManager

    private lateinit var repoA: OfflineInventoryRepository
    private lateinit var repoB: OfflineInventoryRepository
    private lateinit var shoppingA: OfflineShoppingRepository
    private lateinit var shoppingB: OfflineShoppingRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val options =
            FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:1234567890abcdef")
                .setApiKey("fake-api-key")
                .setProjectId("demo-queda-s1")
                .build()

        appA =
            try {
                FirebaseApp.getInstance("userA")
            } catch (_: Exception) {
                FirebaseApp.initializeApp(context, options, "userA")
            }
        appB =
            try {
                FirebaseApp.getInstance("userB")
            } catch (_: Exception) {
                FirebaseApp.initializeApp(context, options, "userB")
            }

        authA = FirebaseAuth.getInstance(appA)
        authA.useEmulator("10.0.2.2", 9099)

        authB = FirebaseAuth.getInstance(appB)
        authB.useEmulator("10.0.2.2", 9099)

        firestoreA = FirebaseFirestore.getInstance(appA)
        firestoreA.useEmulator("10.0.2.2", 8080)

        firestoreB = FirebaseFirestore.getInstance(appB)
        firestoreB.useEmulator("10.0.2.2", 8080)

        dbA = Room.inMemoryDatabaseBuilder(context, QuedaDatabase::class.java).allowMainThreadQueries().build()
        dbB = Room.inMemoryDatabaseBuilder(context, QuedaDatabase::class.java).allowMainThreadQueries().build()

        syncA = HouseholdSyncEngine(firestoreA, authA, dbA, dbA.syncDao())
        syncB = HouseholdSyncEngine(firestoreB, authB, dbB, dbB.syncDao())

        shoppingSyncA = ShoppingSyncEngine(firestoreA, authA, dbA, dbA.shoppingDao())
        shoppingSyncB = ShoppingSyncEngine(firestoreB, authB, dbB, dbB.shoppingDao())

        managerA = FirebaseHouseholdManager(authA, firestoreA, dbA, dbA.syncDao(), syncA, shoppingSyncA)
        managerB = FirebaseHouseholdManager(authB, firestoreB, dbB, dbB.syncDao(), syncB, shoppingSyncB)

        repoA = OfflineInventoryRepository(dbA, dbA.inventoryDao(), dbA.syncDao(), managerA, syncA)
        repoB = OfflineInventoryRepository(dbB, dbB.inventoryDao(), dbB.syncDao(), managerB, syncB)
        shoppingA = OfflineShoppingRepository(dbA, dbA.shoppingDao(), shoppingSyncA)
        shoppingB = OfflineShoppingRepository(dbB, dbB.shoppingDao(), shoppingSyncB)
    }

    @After
    fun tearDown() {
        if (::syncA.isInitialized) syncA.stop()
        if (::syncB.isInitialized) syncB.stop()
        if (::shoppingSyncA.isInitialized) shoppingSyncA.stop()
        if (::shoppingSyncB.isInitialized) shoppingSyncB.stop()
        if (::managerA.isInitialized) managerA.signOut()
        if (::managerB.isInitialized) managerB.signOut()
        if (::dbA.isInitialized) dbA.close()
        if (::dbB.isInitialized) dbB.close()
    }

    @Test
    fun fullHouseholdSyncLifecycleTest(): Unit =
        runBlocking {
            withContext(Dispatchers.IO) {
                val emailA = "usera_${UUID.randomUUID()}@test.com"
                val emailB = "userb_${UUID.randomUUID()}@test.com"

                // 1. User A Registers & Creates Household
                managerA.register(emailA, "password123")
                waitUntil("User A ChooseHousehold") {
                    managerA.state.value is HouseholdSession.ChooseHousehold
                }
                managerA.createHousehold("Casa Sync")
                waitUntil("User A Active") {
                    managerA.state.value is HouseholdSession.Active
                }
                val activeA = managerA.state.value as HouseholdSession.Active
                val householdId = activeA.id
                assertEquals("Casa Sync", activeA.name)

                // 2. User A Creates Invite Code
                managerA.createInvite()
                waitUntil("User A InviteCode") {
                    managerA.inviteCode.value != null
                }
                val inviteCode = checkNotNull(managerA.inviteCode.value)

                // 3. User B Registers & Joins Household via Invite Code
                managerB.register(emailB, "password123")
                waitUntil("User B ChooseHousehold") {
                    managerB.state.value is HouseholdSession.ChooseHousehold
                }
                managerB.joinHousehold(inviteCode)
                waitUntil("User B Active") {
                    managerB.state.value is HouseholdSession.Active
                }
                val activeB = managerB.state.value as HouseholdSession.Active
                assertEquals(householdId, activeB.id)

                // Shopping is visible on the other device and the purchased state is reversible.
                assertEquals(
                    ShoppingResult.Saved,
                    shoppingA.add(householdId = managerA.currentHouseholdId(), name = "Pan", normalizedName = "pan"),
                )
                waitUntil("User B sees shopping item") {
                    shoppingB.observe(managerB.currentHouseholdId()).first().any { it.name == "Pan" }
                }
                val shoppingId = shoppingB.observe(managerB.currentHouseholdId()).first().single { it.name == "Pan" }.id
                assertEquals(ShoppingResult.Saved, shoppingB.setPurchased(managerB.currentHouseholdId(), shoppingId, true))
                waitUntil("User A sees purchased") {
                    shoppingA.observe(managerA.currentHouseholdId()).first().any { it.id == shoppingId && it.purchased }
                }
                assertEquals(ShoppingResult.Saved, shoppingA.setPurchased(managerA.currentHouseholdId(), shoppingId, false))
                waitUntil("User B sees pending again") {
                    shoppingB.observe(managerB.currentHouseholdId()).first().any { it.id == shoppingId && !it.purchased }
                }

                // Two offline additions with the same normalized name converge on one entry.
                shoppingSyncB.stop()
                assertEquals(ShoppingResult.Saved, shoppingB.add(managerB.currentHouseholdId(), "Café", "café"))
                assertEquals(ShoppingResult.Saved, shoppingA.add(managerA.currentHouseholdId(), "Café", "café"))
                waitUntil("User A uploaded Café") { dbA.shoppingDao().pending(householdId).isEmpty() }
                shoppingSyncB.start(householdId)
                waitUntil("Duplicate offline adds converge") {
                    shoppingB.observe(managerB.currentHouseholdId()).first().count { it.normalizedName == "café" } == 1 &&
                        dbB.shoppingDao().pending(householdId).isEmpty()
                }

                // 4. Alta visible para ambos (User A adds "Arroz", 2 kg)
                val hIdA = managerA.currentHouseholdId()
                val productArroz =
                    Product(
                        id = ProductId.from("p-arroz"),
                        householdId = hIdA,
                        name = (ProductName.create("Arroz") as ProductNameCreationResult.Success).productName,
                        barcode = null,
                    )
                val stockArroz =
                    StockItem(
                        id = StockItemId.from("s-arroz"),
                        householdId = hIdA,
                        productId = productArroz.id,
                        quantity = ExactQuantity.of("2", MeasurementUnit.KILOGRAM),
                    )
                val addResult = repoA.addExactInventoryItem(productArroz, stockArroz)
                assertEquals(AddExactItemRepositoryResult.Added, addResult)

                // Verify User B sees "Arroz" synced in DB B
                waitUntil(label = "User B sees Arroz", timeoutMs = 15000) {
                    val listB = repoB.observeExactInventoryItems(managerB.currentHouseholdId()).first()
                    listB.any { it.product.name.displayValue == "Arroz" }
                }
                val listB1 = repoB.observeExactInventoryItems(managerB.currentHouseholdId()).first()
                val arrozB = listB1.first { it.product.name.displayValue == "Arroz" }
                assertEquals("2", (arrozB.stockItem.quantity as ExactQuantity).amount.toPlainString())

                // 5. Consumo concurrente (User A consumes 1 kg)
                val consumeRes =
                    repoA.consumeExactQuantity(
                        StockItemId.from("s-arroz"),
                        ExactQuantity.of("1", MeasurementUnit.KILOGRAM),
                    )
                assertTrue(consumeRes is QuantityMutationResult.Success)

                // Verify User B receives consumed quantity (1 kg)
                waitUntil(label = "User B sees 1 kg", timeoutMs = 15000) {
                    val items = repoB.observeExactInventoryItems(managerB.currentHouseholdId()).first()
                    val item = items.firstOrNull { it.stockItem.id.value == "s-arroz" }
                    (item?.stockItem?.quantity as? ExactQuantity)?.amount?.toPlainString() == "1"
                }

                // 6. Corrección (User B corrects quantity to 5 kg)
                val correctRes =
                    repoB.correctExactQuantity(
                        StockItemId.from("s-arroz"),
                        ExactQuantity.of("5", MeasurementUnit.KILOGRAM),
                    )
                assertTrue(correctRes is QuantityMutationResult.Success)

                // Verify User A receives corrected quantity (5 kg)
                waitUntil(label = "User A sees 5 kg", timeoutMs = 15000) {
                    val items = repoA.observeExactInventoryItems(managerA.currentHouseholdId()).first()
                    val item = items.firstOrNull { it.stockItem.id.value == "s-arroz" }
                    (item?.stockItem?.quantity as? ExactQuantity)?.amount?.toPlainString() == "5"
                }

                // 7. Presencia (User A adds "Sal" presence item, User B toggles)
                val productSal =
                    Product(
                        id = ProductId.from("p-sal"),
                        householdId = hIdA,
                        name = (ProductName.create("Sal") as ProductNameCreationResult.Success).productName,
                        barcode = null,
                    )
                val stockSal =
                    StockItem(
                        id = StockItemId.from("s-sal"),
                        householdId = hIdA,
                        productId = productSal.id,
                        quantity = PresenceQuantity(true),
                    )
                repoA.addExactInventoryItem(productSal, stockSal)

                // User B receives "Sal" (Hay)
                waitUntil(label = "User B sees Sal", timeoutMs = 15000) {
                    val items = repoB.observeExactInventoryItems(managerB.currentHouseholdId()).first()
                    items.any { it.product.name.displayValue == "Sal" }
                }

                // User B toggles presence to false ("No hay")
                val presenceRes = repoB.setPresence(StockItemId.from("s-sal"), false)
                assertTrue(presenceRes is QuantityMutationResult.Success)

                // User A receives "No hay"
                waitUntil(label = "User A sees Sal No hay", timeoutMs = 15000) {
                    val items = repoA.observeExactInventoryItems(managerA.currentHouseholdId()).first()
                    val salA = items.firstOrNull { it.product.name.displayValue == "Sal" }
                    (salA?.stockItem?.quantity as? PresenceQuantity)?.isPresent == false
                }

                // 8. Cola sin conexión y reconexión
                syncB.stop() // User B goes offline
                val hIdB = managerB.currentHouseholdId()
                val productLentejas =
                    Product(
                        id = ProductId.from("p-lentejas"),
                        householdId = hIdB,
                        name = (ProductName.create("Lentejas") as ProductNameCreationResult.Success).productName,
                        barcode = null,
                    )
                val stockLentejas =
                    StockItem(
                        id = StockItemId.from("s-lentejas"),
                        householdId = hIdB,
                        productId = productLentejas.id,
                        quantity = ExactQuantity.of("1", MeasurementUnit.KILOGRAM),
                    )
                repoB.addExactInventoryItem(productLentejas, stockLentejas)

                // User B has it locally in DB B
                val localListB = repoB.observeExactInventoryItems(managerB.currentHouseholdId()).first()
                assertTrue(localListB.any { it.product.name.displayValue == "Lentejas" })

                // User A in DB A does NOT have it yet
                val listA1 = repoA.observeExactInventoryItems(managerA.currentHouseholdId()).first()
                assertTrue(listA1.none { it.product.name.displayValue == "Lentejas" })

                // User B reconnects
                syncB.start(householdId)

                // User A receives "Lentejas" after syncB flushes pending queue
                waitUntil(label = "User A sees Lentejas", timeoutMs = 15000) {
                    val listA2 = repoA.observeExactInventoryItems(managerA.currentHouseholdId()).first()
                    listA2.any { it.product.name.displayValue == "Lentejas" }
                }

                // 9. Actualización manual
                managerA.refresh()
                waitUntil(label = "Manager A updated", timeoutMs = 15000) {
                    managerA.syncStatus.value == HouseholdSyncStatus.UPDATED
                }

                // 10. Arranque con datos locales cuando no hay red (Offline Room reads)
                val itemsLocalA = dbA.inventoryDao().observeExactInventoryItems(householdId).first()
                assertTrue(itemsLocalA.isNotEmpty())

                // The authenticated household can reopen from Firestore cache while network is disabled.
                firestoreB.disableNetwork().awaitTask()
                managerB.start()
                waitUntil("User B reopens household offline") { managerB.state.value is HouseholdSession.Active }
                assertTrue(repoB.observeExactInventoryItems(managerB.currentHouseholdId()).first().isNotEmpty())
                firestoreB.enableNetwork().awaitTask()
            }
        }

    private suspend fun waitUntil(
        label: String = "condition",
        timeoutMs: Long = 15000,
        condition: suspend () -> Boolean,
    ) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw IllegalStateException("Timed out waiting for '$label' after $timeoutMs ms")
            }
            delay(200)
        }
    }
}
