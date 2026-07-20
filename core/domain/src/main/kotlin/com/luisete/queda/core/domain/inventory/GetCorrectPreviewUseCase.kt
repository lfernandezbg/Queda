package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import javax.inject.Inject

class GetCorrectPreviewUseCase
    @Inject
    constructor() {
        operator fun invoke(
            current: ExactQuantity,
            newQuantity: ExactQuantity,
        ): DomainResult<ExactQuantity> = QuantityOperations.correct(current, newQuantity.amount, newQuantity.unit)
    }
