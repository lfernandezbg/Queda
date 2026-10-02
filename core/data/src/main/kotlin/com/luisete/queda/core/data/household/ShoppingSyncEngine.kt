@file:Suppress("detekt:TooGenericExceptionCaught", "detekt:MaxLineLength")

package com.luisete.queda.core.data.household

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.luisete.queda.core.database.PendingShoppingOperationEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.ShoppingDao
import com.luisete.queda.core.database.ShoppingEntryEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingSyncEngine
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
        private val database: QuedaDatabase,
        private val dao: ShoppingDao,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutex = Mutex()
        private var listener: ListenerRegistration? = null

        @Volatile private var activeId: String? = null
        private val _status = MutableStateFlow(HouseholdSyncStatus.PENDING)
        val status: StateFlow<HouseholdSyncStatus> = _status

        fun start(householdId: String) {
            stop()
            activeId = householdId
            _status.value = HouseholdSyncStatus.PENDING
            listener =
                firestore.collection("households").document(householdId).collection("shopping")
                    .addSnapshotListener { snapshot, error ->
                        if (activeId != householdId) return@addSnapshotListener
                        if (error != null) {
                            fail(error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            if (snapshot.metadata.isFromCache) _status.value = HouseholdSyncStatus.OFFLINE
                            scope.launch {
                                try {
                                    snapshot.documents.forEach { merge(householdId, it) }
                                    if (!snapshot.metadata.isFromCache) flush()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    if (activeId == householdId) fail(e)
                                }
                            }
                        }
                    }
            flush()
        }

        fun stop() {
            activeId = null
            listener?.remove()
            listener = null
        }

        fun onLocalChange() {
            _status.value = HouseholdSyncStatus.PENDING
            flush()
        }

        fun refresh() = flush()

        private fun flush() {
            val householdId = activeId ?: return
            scope.launch {
                mutex.withLock {
                    if (activeId != householdId || auth.currentUser == null) return@withLock
                    try {
                        for (operation in dao.pending(householdId)) {
                            if (activeId != householdId) return@withLock
                            upload(operation)
                            dao.acknowledge(operation.id)
                            val remote = shopping(householdId).document(operation.entryId).get(Source.SERVER).awaitTask()
                            merge(householdId, remote)
                        }
                        val snapshot = shopping(householdId).get(Source.SERVER).awaitTask()
                        snapshot.documents.forEach { merge(householdId, it) }
                        if (activeId == householdId) _status.value = HouseholdSyncStatus.UPDATED
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (activeId == householdId) fail(e)
                    }
                }
            }
        }

        private suspend fun upload(op: PendingShoppingOperationEntity) {
            val uid = checkNotNull(auth.currentUser?.uid)
            val house = firestore.collection("households").document(op.householdId)
            val entryRef = house.collection("shopping").document(op.entryId)
            val receipt = entryRef.collection("applied").document(op.id)
            val local = checkNotNull(dao.byId(op.householdId, op.entryId))
            firestore.runTransaction { transaction ->
                val applied = transaction.get(receipt)
                val remote = transaction.get(entryRef)
                if (!applied.exists()) {
                    when (op.action) {
                        "ADD" -> {
                            if (remote.exists()) {
                                check(remote.getString("normalizedName") == local.normalizedName)
                                return@runTransaction Unit
                            }
                            transaction.set(
                                entryRef,
                                mapOf(
                                    "displayName" to local.displayName,
                                    "normalizedName" to local.normalizedName,
                                    "purchased" to false,
                                    "createdAt" to local.createdAt,
                                    "revision" to 1L,
                                    "updatedBy" to uid,
                                ),
                            )
                        }
                        "SET" -> {
                            check(remote.exists()) { "El producto ya no existe en la lista compartida." }
                            transaction.update(
                                entryRef,
                                mapOf(
                                    "purchased" to op.desiredPurchased,
                                    "revision" to (checkNotNull(remote.getLong("revision")) + 1),
                                    "updatedBy" to uid,
                                ),
                            )
                        }
                        else -> error("Operación de compra desconocida")
                    }
                    transaction.set(receipt, mapOf("uid" to uid))
                }
                Unit
            }.awaitTask()
        }

        private suspend fun merge(
            householdId: String,
            remote: DocumentSnapshot,
        ) {
            if (activeId != householdId || !remote.exists()) return
            database.withTransaction {
                if (dao.pendingForEntry(householdId, remote.id) != 0) return@withTransaction
                val entity =
                    ShoppingEntryEntity(
                        remote.id,
                        householdId,
                        checkNotNull(remote.getString("displayName")),
                        checkNotNull(remote.getString("normalizedName")),
                        checkNotNull(remote.getBoolean("purchased")),
                        checkNotNull(remote.getLong("createdAt")),
                    )
                val current = dao.byId(householdId, entity.id)
                if (current == null) {
                    if (dao.byName(householdId, entity.normalizedName) == null) dao.insert(entity)
                } else {
                    check(current.normalizedName == entity.normalizedName)
                    dao.setPurchased(householdId, entity.id, entity.purchased)
                }
            }
        }

        private fun shopping(id: String) = firestore.collection("households").document(id).collection("shopping")

        private fun fail(error: Exception) {
            _status.value =
                if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    HouseholdSyncStatus.OFFLINE
                } else {
                    HouseholdSyncStatus.ERROR
                }
        }
    }
