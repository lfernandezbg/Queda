package com.luisete.queda.core.domain.shopping

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.shopping.ShoppingEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingUseCasesTest {
    private val householdId = HouseholdId.from("house-1")
    private val repository = RecordingRepository()

    @Test fun addNormalizesNameAndScopesToHousehold() =
        runTest {
            val add = AddShoppingEntry(repository) { householdId }
            val result = add("  Pan   integral  ")
            assertEquals(ShoppingResult.Saved, result)
            assertEquals(Triple(householdId, "Pan integral", "pan integral"), repository.lastAdded)
        }

    @Test fun invalidNameDoesNotPersist() =
        runTest {
            val add = AddShoppingEntry(repository) { householdId }
            add("   ")
            assertEquals(null, repository.lastAdded)
        }

    @Test fun checkedStateIsExplicitRatherThanAToggle() =
        runTest {
            val id = ShoppingEntryId.from("entry-1")
            val set = SetShoppingPurchased(repository) { householdId }
            set(id, true)
            assertEquals(Triple(householdId, id, true), repository.lastPurchased)
        }

    private class RecordingRepository : ShoppingRepository {
        var lastAdded: Triple<HouseholdId, String, String>? = null
        var lastPurchased: Triple<HouseholdId, ShoppingEntryId, Boolean>? = null

        override fun observe(householdId: HouseholdId): Flow<List<ShoppingEntry>> = flowOf(emptyList())

        override suspend fun add(
            householdId: HouseholdId,
            name: String,
            normalizedName: String,
        ): ShoppingResult {
            lastAdded = Triple(householdId, name, normalizedName)
            return ShoppingResult.Saved
        }

        override suspend fun setPurchased(
            householdId: HouseholdId,
            id: ShoppingEntryId,
            purchased: Boolean,
        ): ShoppingResult {
            lastPurchased = Triple(householdId, id, purchased)
            return ShoppingResult.Saved
        }
    }
}
