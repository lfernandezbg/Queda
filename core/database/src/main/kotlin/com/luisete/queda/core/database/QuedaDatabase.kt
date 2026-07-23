package com.luisete.queda.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        StockItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class QuedaDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        private const val DB_VERSION_1 = 1
        private const val DB_VERSION_2 = 2
        private const val DB_VERSION_3 = 3

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
    }
}
