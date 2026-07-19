package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.model.id.HouseholdId
import javax.inject.Inject

class LocalCurrentHouseholdIdProvider
    @Inject
    constructor() : CurrentHouseholdIdProvider {
        override fun currentHouseholdId(): HouseholdId = HouseholdId.from("local-household-v1")
    }
