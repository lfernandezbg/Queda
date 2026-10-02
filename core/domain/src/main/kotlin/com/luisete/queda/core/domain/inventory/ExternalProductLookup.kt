package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.barcode.Barcode

interface ExternalProductLookup {
    suspend fun findByBarcode(barcode: Barcode): ExternalProductResult
}

sealed interface ExternalProductResult {
    data class Found(val name: String) : ExternalProductResult

    data object NotFound : ExternalProductResult

    data object MissingName : ExternalProductResult

    data object Unavailable : ExternalProductResult
}
