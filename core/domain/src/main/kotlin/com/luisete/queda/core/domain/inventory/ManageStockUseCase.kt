package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.FoodType
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.StockQuantity
import java.time.LocalDate
import javax.inject.Inject

enum class StockWriteResult { SAVED, INVALID, STALE, DUPLICATE, STORAGE_FAILURE }

data class SelectedStock(
    val id: String,
    val expectedQuantity: StockQuantity,
    val toConsume: ExactQuantity? = null,
)

interface StockManagementRepository {
    suspend fun editDetails(
        id: String,
        expected: StockDetails,
        desired: StockDetails,
    ): StockWriteResult

    suspend fun editItem(
        id: String,
        expected: StockDetails,
        desired: StockDetails,
        expectedName: String,
        desiredName: String,
    ): StockWriteResult {
        return if (expectedName == desiredName) {
            editDetails(id, expected, desired)
        } else {
            StockWriteResult.INVALID
        }
    }

    suspend fun consumeSelection(items: List<SelectedStock>): StockWriteResult

    suspend fun moveSelection(
        items: List<SelectedStock>,
        locationId: String?,
    ): StockWriteResult

    suspend fun addLot(
        name: String,
        quantity: ExactQuantity,
        details: StockDetails,
    ): StockWriteResult
}

class ManageStockUseCase
    @Inject
    constructor(private val repository: StockManagementRepository) {
        suspend fun addPrepared(
            name: String,
            quantity: String,
            unit: MeasurementUnit,
            details: StockDetails,
        ): StockWriteResult {
            val productName = ProductName.create(name)
            val parsed = ExactQuantityInputParser.parse(quantity, unit)
            val invalidInput =
                productName !is ProductNameCreationResult.Success ||
                    parsed !is ExactQuantityInputResult.Success
            return if (invalidInput ||
                details.foodType != FoodType.PREPARED || !details.isValid(LocalDate.now())
            ) {
                StockWriteResult.INVALID
            } else {
                repository.addLot(
                    (productName as ProductNameCreationResult.Success).productName.displayValue,
                    (parsed as ExactQuantityInputResult.Success).quantity,
                    details,
                )
            }
        }

        suspend fun edit(
            id: String,
            expected: StockDetails,
            desired: StockDetails,
            expectedName: String? = null,
            desiredName: String? = null,
        ): StockWriteResult {
            val invalidName =
                desiredName != null &&
                    ProductName.create(desiredName) !is ProductNameCreationResult.Success
            val invalid = id.isBlank() || !desired.isValid(LocalDate.now()) || invalidName
            return if (invalid) {
                StockWriteResult.INVALID
            } else if (expectedName != null && desiredName != null) {
                repository.editItem(id, expected, desired, expectedName, desiredName.trim())
            } else {
                repository.editDetails(id, expected, desired)
            }
        }

        suspend fun consume(items: List<SelectedStock>): StockWriteResult =
            if (!validSelection(items) ||
                items.any {
                    it.toConsume != null && it.expectedQuantity !is ExactQuantity
                }
            ) {
                StockWriteResult.INVALID
            } else {
                repository.consumeSelection(items)
            }

        suspend fun move(
            items: List<SelectedStock>,
            locationId: String?,
        ): StockWriteResult =
            if (!validSelection(items) || locationId?.isBlank() == true) {
                StockWriteResult.INVALID
            } else {
                repository.moveSelection(items, locationId)
            }

        private fun validSelection(items: List<SelectedStock>): Boolean =
            items.size in 1..MAX_SELECTION && items.map { it.id }.distinct().size == items.size &&
                items.none { it.id.isBlank() }

        companion object {
            const val MAX_SELECTION = 8
        }
    }
