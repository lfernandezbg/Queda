package com.luisete.queda.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingDaoTest {
    @Test fun householdIsolationAndPurchaseUpdate() =
        runBlocking {
            val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QuedaDatabase::class.java).build()
            try {
                val dao = db.shoppingDao()
                dao.insert(ShoppingEntryEntity("one", "house-a", "Pan", "pan", false, 1))
                dao.insert(ShoppingEntryEntity("two", "house-b", "Leche", "leche", false, 2))
                dao.setPurchased("house-b", "one", true)
                assertEquals(false, dao.byId("house-a", "one")?.purchased)
                assertNull(dao.byId("house-a", "two"))
                dao.setPurchased("house-a", "one", true)
                assertEquals(true, dao.observe("house-a").first().single().purchased)
            } finally {
                db.close()
            }
        }

    @Test fun pendingOperationsAreOrderedAndKeptPerHousehold() =
        runBlocking {
            val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QuedaDatabase::class.java).build()
            try {
                val dao = db.shoppingDao()
                dao.enqueue(PendingShoppingOperationEntity("second", "house-a", "one", "SET", true, 20))
                dao.enqueue(PendingShoppingOperationEntity("first", "house-a", "one", "ADD", false, 10))
                dao.enqueue(PendingShoppingOperationEntity("other", "house-b", "two", "ADD", false, 1))
                assertEquals(listOf("first", "second"), dao.pending("house-a").map { it.id })
                dao.acknowledge("first")
                assertEquals(listOf("second"), dao.pending("house-a").map { it.id })
            } finally {
                db.close()
            }
        }
}
