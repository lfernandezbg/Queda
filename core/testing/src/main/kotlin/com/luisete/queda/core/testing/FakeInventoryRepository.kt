package com.luisete.queda.core.testing

import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.domain.inventory.InventoryRepository
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.Collections

class FakeInventoryRepository : InventoryRepository {
    private val itemsFlow = MutableSharedFlow<List<InventoryItem>>(replay = 1)
    private var addResult: AddExactItemRepositoryResult = AddExactItemRepositoryResult.Added
    private var flowError: Throwable? = null

    val observedHouseholdIds: MutableList<HouseholdId> = Collections.synchronizedList(mutableListOf())
    var observeSubscriptionsCount = 0
        private set

    val addedProducts: MutableList<Product> = Collections.synchronizedList(mutableListOf())
    val addedStockItems: MutableList<StockItem> = Collections.synchronizedList(mutableListOf())
    var addCallsCount = 0
        private set

    private var nextAddDeferred: CompletableDeferred<Unit>? = null

    fun emit(items: List<InventoryItem>) {
        itemsFlow.tryEmit(items)
    }

    fun setAddResult(result: AddExactItemRepositoryResult) {
        addResult = result
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

    fun suspendNextAdd() {
        nextAddDeferred = CompletableDeferred()
    }

    fun completeSuspendedAdd() {
        nextAddDeferred?.complete(Unit)
        nextAddDeferred = null
    }

    override suspend fun addExactInventoryItem(
        product: Product,
        stockItem: StockItem,
    ): AddExactItemRepositoryResult {
        addCallsCount++
        nextAddDeferred?.await()
        addedProducts.add(product)
        addedStockItems.add(stockItem)
        return addResult
    }
}
