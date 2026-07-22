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
    version = 2,
    exportSchema = true,
)
abstract class QuedaDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_barcode ON products (barcode)")
                }
            }
    }
}
