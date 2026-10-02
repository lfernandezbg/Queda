package com.luisete.queda.core.domain.shopping

import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import javax.inject.Inject

class ObserveShoppingEntries
    @Inject
    constructor(
        private val repository: ShoppingRepository,
        private val household: CurrentHouseholdIdProvider,
    ) {
        operator fun invoke() = repository.observe(household.currentHouseholdId())
    }

class AddShoppingEntry
    @Inject
    constructor(
        private val repository: ShoppingRepository,
        private val household: CurrentHouseholdIdProvider,
    ) {
        suspend operator fun invoke(rawName: String): ShoppingResult =
            when (val result = ProductName.create(rawName)) {
                is ProductNameCreationResult.Success ->
                    repository.add(
                        householdId = household.currentHouseholdId(),
                        name = result.productName.displayValue,
                        normalizedName = result.productName.normalizedKey,
                    )
                else -> ShoppingResult.StorageFailure
            }
    }

class SetShoppingPurchased
    @Inject
    constructor(
        private val repository: ShoppingRepository,
        private val household: CurrentHouseholdIdProvider,
    ) {
        suspend operator fun invoke(
            id: ShoppingEntryId,
            purchased: Boolean,
        ): ShoppingResult = repository.setPurchased(household.currentHouseholdId(), id, purchased)
    }
