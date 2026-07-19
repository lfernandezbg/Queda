package com.luisete.queda.core.data.di

import com.luisete.queda.core.data.inventory.LocalCurrentHouseholdIdProvider
import com.luisete.queda.core.data.inventory.OfflineInventoryRepository
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.InventoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindInventoryRepository(impl: OfflineInventoryRepository): InventoryRepository

    @Binds
    @Singleton
    fun bindCurrentHouseholdIdProvider(impl: LocalCurrentHouseholdIdProvider): CurrentHouseholdIdProvider
}
