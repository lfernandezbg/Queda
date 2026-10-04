package com.luisete.queda.core.data.di

import com.luisete.queda.core.data.auth.FirebaseAccountRepository
import com.luisete.queda.core.data.household.FirebaseHouseholdManager
import com.luisete.queda.core.data.inventory.MlKitReceiptTextReader
import com.luisete.queda.core.data.inventory.OfflineInventoryRepository
import com.luisete.queda.core.data.inventory.OfflineLocationsRepository
import com.luisete.queda.core.data.inventory.OfflineReceiptRepository
import com.luisete.queda.core.data.inventory.OfflineStockManagementRepository
import com.luisete.queda.core.data.inventory.OpenFoodFactsProductLookup
import com.luisete.queda.core.data.shopping.OfflineShoppingRepository
import com.luisete.queda.core.domain.auth.AccountRepository
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.ExternalProductLookup
import com.luisete.queda.core.domain.inventory.InventoryRepository
import com.luisete.queda.core.domain.inventory.LocationsRepository
import com.luisete.queda.core.domain.inventory.ReceiptRepository
import com.luisete.queda.core.domain.inventory.ReceiptTextReader
import com.luisete.queda.core.domain.inventory.StockManagementRepository
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
    fun bindLocations(impl: OfflineLocationsRepository): LocationsRepository

    @Binds
    fun bindStockManagement(impl: OfflineStockManagementRepository): StockManagementRepository

    @Binds
    fun bindReceipt(impl: OfflineReceiptRepository): ReceiptRepository

    @Binds
    fun bindReceiptReader(impl: MlKitReceiptTextReader): ReceiptTextReader

    @Binds
    fun bindAccount(impl: FirebaseAccountRepository): AccountRepository

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
