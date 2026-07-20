package com.luisete.queda.core.testing

import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.domain.inventory.InventoryRepository
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.quantity.ExactQuantity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.Collections

class FakeInventoryRepository : InventoryRepository {
    private val itemsFlow = MutableSharedFlow<List<InventoryItem>>(replay = 1)
    private var addResult: AddExactItemRepositoryResult = AddExactItemRepositoryResult.Added
    private var mutationResult: QuantityMutationResult? = null
    private var flowError: Throwable? = null

    val observedHouseholdIds: MutableList<HouseholdId> = Collections.synchronizedList(mutableListOf())
    var observeSubscriptionsCount = 0
        private set

    val addedProducts: MutableList<Product> = Collections.synchronizedList(mutableListOf())
    val addedStockItems: MutableList<StockItem> = Collections.synchronizedList(mutableListOf())
    var addCallsCount = 0
        private set

    val consumedExactQuantities: MutableList<Pair<StockItemId, ExactQuantity>> =
        Collections.synchronizedList(mutableListOf())
    val correctedExactQuantities: MutableList<Pair<StockItemId, ExactQuantity>> =
        Collections.synchronizedList(mutableListOf())

    private var nextMutationDeferred: CompletableDeferred<Unit>? = null

    fun emit(items: List<InventoryItem>) {
        itemsFlow.tryEmit(items)
    }

    fun setAddResult(result: AddExactItemRepositoryResult) {
        addResult = result
    }

    fun setMutationResult(result: QuantityMutationResult) {
        mutationResult = result
    }

    fun setFlowError(error: Throwable?) {
        flowError = error
    }

    override fun observeExactInventoryItems(householdId: HouseholdId): Flow<List<InventoryItem>> {
        return (
            if (flowError != null) {
                flow { throw flowError!! }
            } else {
                itemsFlow
            }
        ).onStart {
            observeSubscriptionsCount++
            observedHouseholdIds.add(householdId)
        }
    }

    fun suspendNextMutation() {
        nextMutationDeferred = CompletableDeferred()
    }

    fun completeSuspendedMutation() {
        nextMutationDeferred?.complete(Unit)
        nextMutationDeferred = null
    }

    override suspend fun addExactInventoryItem(
        product: Product,
        stockItem: StockItem,
    ): AddExactItemRepositoryResult {
        addCallsCount++
        nextMutationDeferred?.await()
        addedProducts.add(product)
        addedStockItems.add(stockItem)
        return addResult
    }

    override suspend fun consumeExactQuantity(
        stockItemId: StockItemId,
        toConsume: ExactQuantity,
    ): QuantityMutationResult {
        nextMutationDeferred?.await()
        consumedExactQuantities.add(stockItemId to toConsume)
        return mutationResult ?: QuantityMutationResult.Failure(DomainError.StorageFailure)
    }

    override suspend fun correctExactQuantity(
        stockItemId: StockItemId,
        newQuantity: ExactQuantity,
    ): QuantityMutationResult {
        nextMutationDeferred?.await()
        correctedExactQuantities.add(stockItemId to newQuantity)
        return mutationResult ?: QuantityMutationResult.Failure(DomainError.StorageFailure)
    }
}
