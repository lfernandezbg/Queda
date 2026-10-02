@file:Suppress("detekt:TooGenericExceptionCaught", "detekt:SwallowedException", "detekt:MaxLineLength", "detekt:MagicNumber")

package com.luisete.queda.core.data.shopping

import com.luisete.queda.core.data.household.ShoppingSyncEngine
import com.luisete.queda.core.database.PendingShoppingOperationEntity
import com.luisete.queda.core.database.ShoppingDao
import com.luisete.queda.core.database.ShoppingEntryEntity
import com.luisete.queda.core.domain.shopping.ShoppingRepository
import com.luisete.queda.core.domain.shopping.ShoppingResult
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.shopping.ShoppingEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class OfflineShoppingRepository
    @Inject
    constructor(
        private val dao: ShoppingDao,
        private val sync: ShoppingSyncEngine,
    ) : ShoppingRepository {
        override fun observe(householdId: HouseholdId) =
            dao.observe(householdId.value).map { entries ->
                entries.map { entry ->
                    ShoppingEntry(
                        ShoppingEntryId.from(entry.id),
                        HouseholdId.from(entry.householdId),
                        entry.displayName,
                        entry.normalizedName,
                        entry.purchased,
                        entry.createdAt,
                    )
                }
            }

        override suspend fun add(
            householdId: HouseholdId,
            name: String,
            normalizedName: String,
        ): ShoppingResult =
            try {
                if (dao.byName(householdId.value, normalizedName) != null) {
                    ShoppingResult.AlreadyExists
                } else {
                    val id =
                        MessageDigest.getInstance("SHA-256")
                            .digest("${householdId.value}:$normalizedName".toByteArray(Charsets.UTF_8))
                            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    val createdAt = System.currentTimeMillis()
                    dao.insert(ShoppingEntryEntity(id, householdId.value, name, normalizedName, false, createdAt))
                    dao.enqueue(
                        PendingShoppingOperationEntity(UUID.randomUUID().toString(), householdId.value, id, "ADD", false, createdAt),
                    )
                    try {
                        sync.onLocalChange()
                    } catch (e: Exception) {
                        android.util.Log.w("Shopping", "Sync failed", e)
                    }
                    ShoppingResult.Saved
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ShoppingResult.StorageFailure
            }

        override suspend fun setPurchased(
            householdId: HouseholdId,
            id: ShoppingEntryId,
            purchased: Boolean,
        ): ShoppingResult =
            try {
                val current = dao.byId(householdId.value, id.value) ?: return ShoppingResult.NotFound
                if (current.purchased == purchased) return ShoppingResult.Saved
                dao.setPurchased(householdId.value, id.value, purchased)
                dao.enqueue(
                    PendingShoppingOperationEntity(
                        UUID.randomUUID().toString(),
                        householdId.value,
                        id.value,
                        "SET",
                        purchased,
                        System.currentTimeMillis(),
                    ),
                )
                try {
                    sync.onLocalChange()
                } catch (e: Exception) {
                    android.util.Log.w("Shopping", "Sync failed", e)
                }
                ShoppingResult.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ShoppingResult.StorageFailure
            }
    }
