package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class StockItemId private constructor(val value: String) {
    companion object {
        fun newId(): StockItemId = StockItemId(UUID.randomUUID().toString())

        fun from(value: String): StockItemId {
            require(value.isNotBlank()) { "StockItemId cannot be empty or blank" }
            return StockItemId(value)
        }
    }
}
