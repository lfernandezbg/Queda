package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import javax.inject.Inject

class CorrectExactQuantityUseCase
    @Inject
    constructor(
        private val inventoryRepository: InventoryRepository,
    ) {
        suspend operator fun invoke(
            stockItemId: StockItemId,
            newQuantity: ExactQuantity,
        ): QuantityMutationResult = inventoryRepository.correctExactQuantity(stockItemId, newQuantity)
    }
