package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import javax.inject.Inject

class AddExactQuantityUseCase
    @Inject
    constructor(private val repository: InventoryRepository) {
        suspend operator fun invoke(
            id: StockItemId,
            quantity: ExactQuantity,
        ): QuantityMutationResult = repository.addExactQuantity(id, quantity)
    }
