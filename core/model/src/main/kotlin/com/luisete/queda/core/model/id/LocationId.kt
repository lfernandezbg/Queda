package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class LocationId private constructor(val value: String) {
    companion object {
        fun newId(): LocationId = LocationId(UUID.randomUUID().toString())

        fun from(value: String): LocationId {
            require(value.isNotBlank()) { "LocationId cannot be empty or blank" }
            return LocationId(value)
        }
    }
}
