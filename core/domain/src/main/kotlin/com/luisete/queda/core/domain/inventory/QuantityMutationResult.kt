package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.quantity.StockQuantity

sealed interface QuantityMutationResult {
    data class Success(val newQuantity: StockQuantity) : QuantityMutationResult

    data class Failure(val error: DomainError) : QuantityMutationResult
}
