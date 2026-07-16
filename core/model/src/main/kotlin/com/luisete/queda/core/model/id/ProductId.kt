package com.luisete.queda.core.model.id

import java.util.UUID

@JvmInline
value class ProductId private constructor(val value: String) {
    companion object {
        fun from(value: String): ProductId {
            require(value.isNotBlank()) { "ProductId cannot be blank" }
            return ProductId(value)
        }

        fun newId(): ProductId = ProductId(UUID.randomUUID().toString())
    }
}
