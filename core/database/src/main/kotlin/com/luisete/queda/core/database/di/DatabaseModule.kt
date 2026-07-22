package com.luisete.queda.core.database.di

import android.content.Context
import androidx.room.Room
import com.luisete.queda.core.database.InventoryDao
import com.luisete.queda.core.database.QuedaDatabase
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
            )
            .build()

    @Provides
    @Singleton
    fun provideInventoryDao(database: QuedaDatabase): InventoryDao = database.inventoryDao()
}
