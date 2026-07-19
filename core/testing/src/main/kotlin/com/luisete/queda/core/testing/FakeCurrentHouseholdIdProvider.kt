package com.luisete.queda.core.testing

import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.model.id.HouseholdId

class FakeCurrentHouseholdIdProvider : CurrentHouseholdIdProvider {
    var householdId = HouseholdId.from("fake-household")
    var callCount = 0
        private set

    override fun currentHouseholdId(): HouseholdId {
        callCount++
        return householdId
    }
}
