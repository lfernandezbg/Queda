package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class ShoppingEntryId private constructor(val value: String) {
    companion object {
        fun from(value: String): ShoppingEntryId {
            require(value.isNotBlank()) { "ShoppingEntryId cannot be blank" }
            return ShoppingEntryId(value)
        }

        fun newId(): ShoppingEntryId = ShoppingEntryId(UUID.randomUUID().toString())
    }
}
