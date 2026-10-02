package com.luisete.queda.core.model.shopping

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ShoppingEntryId

data class ShoppingEntry(
    val id: ShoppingEntryId,
    val householdId: HouseholdId,
    val name: String,
    val normalizedName: String,
    val purchased: Boolean,
    val createdAt: Long,
)
