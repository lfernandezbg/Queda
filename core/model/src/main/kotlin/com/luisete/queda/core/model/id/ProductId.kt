package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class ProductId private constructor(val value: String) {
    companion object {
        fun newId(): ProductId = ProductId(UUID.randomUUID().toString())

        fun from(value: String): ProductId {
            require(value.isNotBlank()) { "ProductId cannot be empty or blank" }
            return ProductId(value)
        }
    }
}
