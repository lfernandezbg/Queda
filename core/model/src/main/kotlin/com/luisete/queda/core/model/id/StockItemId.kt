package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class StockItemId private constructor(val value: String) {
    companion object {
        fun from(value: String): StockItemId {
            require(value.isNotBlank()) { "StockItemId cannot be blank" }
            return StockItemId(value)
        }

        fun newId(): StockItemId = StockItemId(UUID.randomUUID().toString())
    }
}
