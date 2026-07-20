package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import javax.inject.Inject

class GetConsumePreviewUseCase
    @Inject
    constructor() {
        operator fun invoke(
            available: ExactQuantity,
            toConsume: ExactQuantity,
        ): DomainResult<ExactQuantity> = QuantityOperations.consume(available, toConsume)
    }
