package com.luisete.queda.core.data.di

import com.luisete.queda.core.data.household.FirebaseHouseholdManager
import com.luisete.queda.core.data.inventory.OfflineInventoryRepository
import com.luisete.queda.core.data.inventory.OpenFoodFactsProductLookup
import com.luisete.queda.core.data.shopping.OfflineShoppingRepository
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.ExternalProductLookup
import com.luisete.queda.core.domain.inventory.InventoryRepository
import com.luisete.queda.core.domain.shopping.ShoppingRepository
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
    fun bindCurrentHouseholdIdProvider(impl: FirebaseHouseholdManager): CurrentHouseholdIdProvider

    @Binds
    @Singleton
    fun bindExternalProductLookup(impl: OpenFoodFactsProductLookup): ExternalProductLookup

    @Binds
    @Singleton
    fun bindShoppingRepository(impl: OfflineShoppingRepository): ShoppingRepository
}
