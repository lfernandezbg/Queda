package com.luisete.queda.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            QuedaDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        helper.createDatabase(testDb, 1).use { db ->
            db.execSQL(
                "INSERT INTO products (id, householdId, displayName, normalizedName) " +
                    "VALUES ('p1', 'h1', 'Milk', 'milk')",
            )
            db.execSQL(
                "INSERT INTO stock_items (id, householdId, productId, quantityAmount, quantityUnit) " +
                    "VALUES ('s1', 'h1', 'p1', '10.5', 'LITER')",
            )
        }

        val db = helper.runMigrationsAndValidate(testDb, 2, true, QuedaDatabase.MIGRATION_1_2)

        db.query("SELECT * FROM products WHERE id = 'p1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("h1", cursor.getString(cursor.getColumnIndexOrThrow("householdId")))
            assertEquals("Milk", cursor.getString(cursor.getColumnIndexOrThrow("displayName")))
            assertEquals("milk", cursor.getString(cursor.getColumnIndexOrThrow("normalizedName")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("barcode")))
        }

        db.query("SELECT * FROM stock_items WHERE id = 's1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("p1", cursor.getString(cursor.getColumnIndexOrThrow("productId")))
            assertEquals("10.5", cursor.getString(cursor.getColumnIndexOrThrow("quantityAmount")))
            assertEquals("LITER", cursor.getString(cursor.getColumnIndexOrThrow("quantityUnit")))
        }

        // Insert non-null barcode
        db.execSQL(
            "INSERT INTO products (id, householdId, displayName, normalizedName, barcode) " +
                "VALUES ('p2', 'h1', 'Eggs', 'eggs', '12345678')",
        )
        db.query("SELECT barcode FROM products WHERE id = 'p2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("12345678", cursor.getString(0))
        }

        // Duplicate barcode
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                "INSERT INTO products (id, householdId, displayName, normalizedName, barcode) " +
                    "VALUES ('p3', 'h1', 'Other Eggs', 'othereggs', '12345678')",
            )
        }

        // Multiple NULL barcodes
        db.execSQL(
            "INSERT INTO products (id, householdId, displayName, normalizedName, barcode) " +
                "VALUES ('p4', 'h1', 'Cheese', 'cheese', NULL)",
        )
        db.execSQL(
            "INSERT INTO products (id, householdId, displayName, normalizedName, barcode) " +
                "VALUES ('p5', 'h1', 'Ham', 'ham', NULL)",
        )

        db.query("SELECT COUNT(*) FROM products WHERE barcode IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(3, cursor.getInt(0)) // p1, p4, p5
        }

        db.close()
    }
}
