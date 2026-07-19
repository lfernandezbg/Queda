package com.luisete.queda.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        StockItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class QuedaDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
}
