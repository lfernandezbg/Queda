package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class DomainEventId private constructor(val value: String) {
    companion object {
        fun newId(): DomainEventId = DomainEventId(UUID.randomUUID().toString())

        fun from(value: String): DomainEventId {
            require(value.isNotBlank()) { "DomainEventId cannot be empty or blank" }
            return DomainEventId(value)
        }
    }
}
