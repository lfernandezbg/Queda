package com.luisete.queda.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        StockItemEntity::class,
        PendingSyncOperationEntity::class,
        ShoppingEntryEntity::class,
        PendingShoppingOperationEntity::class,
        LocationEntity::class,
        ReceiptDraftEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class QuedaDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    abstract fun syncDao(): SyncDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun managementDao(): ManagementDao

    companion object {
        private const val DB_VERSION_6 = 6
        val MIGRATION_5_6 =
            object : Migration(DB_VERSION_5, DB_VERSION_6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    StockMigration.migrate(db)
                }
            }

        private const val DB_VERSION_1 = 1
        private const val DB_VERSION_2 = 2
        private const val DB_VERSION_3 = 3
        private const val DB_VERSION_4 = 4
        private const val DB_VERSION_5 = 5

        val MIGRATION_1_2 =
            object : Migration(DB_VERSION_1, DB_VERSION_2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_barcode ON products (barcode)")
                }
            }

        val MIGRATION_2_3 =
            object : Migration(DB_VERSION_2, DB_VERSION_3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `stock_items_new` (
                            `id` TEXT NOT NULL,
                            `householdId` TEXT NOT NULL,
                            `productId` TEXT NOT NULL,
                            `trackingMode` TEXT NOT NULL,
                            `quantityAmount` TEXT,
                            `quantityUnit` TEXT,
                            `isPresent` INTEGER,
                            PRIMARY KEY(`id`),
                            FOREIGN KEY(`productId`) REFERENCES `products`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        INSERT INTO `stock_items_new`
                            (id, householdId, productId, trackingMode, quantityAmount, quantityUnit, isPresent)
                        SELECT id, householdId, productId, 'EXACT', quantityAmount, quantityUnit, NULL
                        FROM stock_items
                        """.trimIndent(),
                    )

                    db.execSQL("DROP TABLE stock_items")
                    db.execSQL("ALTER TABLE stock_items_new RENAME TO stock_items")
                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS `index_stock_items_householdId`
                        ON `stock_items` (`householdId`)
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS `index_stock_items_productId`
                        ON `stock_items` (`productId`)
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_3_4 =
            object : Migration(DB_VERSION_3, DB_VERSION_4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_products_barcode")
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_products_householdId_barcode " +
                            "ON products (householdId, barcode)",
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS pending_sync_operations (
                            id TEXT NOT NULL PRIMARY KEY,
                            householdId TEXT NOT NULL,
                            stockItemId TEXT NOT NULL,
                            action TEXT NOT NULL,
                            payload TEXT NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_pending_sync_operations_householdId_createdAt " +
                            "ON pending_sync_operations (householdId, createdAt)",
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(DB_VERSION_4, DB_VERSION_5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS shopping_entries (
                            id TEXT NOT NULL PRIMARY KEY,
                            householdId TEXT NOT NULL,
                            displayName TEXT NOT NULL,
                            normalizedName TEXT NOT NULL,
                            purchased INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_entries_householdId_normalizedName
                        ON shopping_entries (householdId, normalizedName)
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS pending_shopping_operations (
                            id TEXT NOT NULL PRIMARY KEY,
                            householdId TEXT NOT NULL,
                            entryId TEXT NOT NULL,
                            action TEXT NOT NULL,
                            desiredPurchased INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS index_pending_shopping_operations_householdId_createdAt
                        ON pending_shopping_operations (householdId, createdAt)
                        """.trimIndent(),
                    )
                }
            }
    }
}
