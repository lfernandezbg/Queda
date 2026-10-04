package com.luisete.queda.core.database.di

import android.content.Context
import androidx.room.Room
import com.luisete.queda.core.database.InventoryDao
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.ShoppingDao
import com.luisete.queda.core.database.SyncDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideQuedaDatabase(
        @ApplicationContext context: Context,
    ): QuedaDatabase =
        Room.databaseBuilder(
            context,
            QuedaDatabase::class.java,
            "queda-database",
        )
            .addMigrations(
                QuedaDatabase.MIGRATION_1_2,
                QuedaDatabase.MIGRATION_2_3,
                QuedaDatabase.MIGRATION_3_4,
                QuedaDatabase.MIGRATION_4_5,
                QuedaDatabase.MIGRATION_5_6,
            )
            .build()

    @Provides
    @Singleton
    fun provideInventoryDao(database: QuedaDatabase): InventoryDao = database.inventoryDao()

    @Provides
    fun provideSyncDao(database: QuedaDatabase): SyncDao = database.syncDao()

    @Provides
    fun provideShoppingDao(database: QuedaDatabase): ShoppingDao = database.shoppingDao()
}
