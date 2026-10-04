package com.luisete.queda.core.model.inventory

import java.time.LocalDate

enum class FoodType { FOOD, PREPARED }

data class StockDetails(
    val locationId: String? = null,
    val foodType: FoodType = FoodType.FOOD,
    val label: String? = null,
    val preparedOn: LocalDate? = null,
    val bestBefore: LocalDate? = null,
) {
    fun isValid(today: LocalDate): Boolean =
        (locationId == null || locationId.isNotBlank()) &&
            (label == null || label.isNotBlank() && label.length <= MAX_LABEL_LENGTH) &&
            (preparedOn == null || foodType == FoodType.PREPARED && !preparedOn.isAfter(today)) &&
            (preparedOn == null || bestBefore == null || !bestBefore.isBefore(preparedOn))

    companion object {
        private const val MAX_LABEL_LENGTH = 100
    }
}

data class StorageLocation(
    val id: String,
    val name: String,
    val archived: Boolean = false,
    val conflicted: Boolean = false,
)
