package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class HouseholdId private constructor(val value: String) {
    companion object {
        fun from(value: String): HouseholdId {
            require(value.isNotBlank()) { "HouseholdId cannot be blank" }
            return HouseholdId(value)
        }

        fun newId(): HouseholdId = HouseholdId(UUID.randomUUID().toString())
    }
}
