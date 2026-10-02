package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.HouseholdId

fun interface CurrentHouseholdIdProvider {
    fun currentHouseholdId(): HouseholdId
}
