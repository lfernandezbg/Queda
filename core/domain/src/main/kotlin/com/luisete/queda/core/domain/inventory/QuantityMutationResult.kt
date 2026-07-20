package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.quantity.ExactQuantity

sealed interface QuantityMutationResult {
    data class Success(val newQuantity: ExactQuantity) : QuantityMutationResult

    data class Failure(val error: DomainError) : QuantityMutationResult
}
