@file:Suppress(
    "detekt:LongMethod",
    "detekt:CyclomaticComplexMethod",
    "detekt:TooManyFunctions",
    "detekt:TooGenericExceptionCaught",
    "detekt:InstanceOfCheckForException",
    "detekt:MaxLineLength",
    "detekt:MagicNumber",
)

package com.luisete.queda.core.data.household

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.luisete.queda.core.database.PendingSyncOperationEntity
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.database.SyncDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class HouseholdSyncStatus {
    UPDATED,
    PENDING,
    OFFLINE,
    ERROR,
}

@Singleton
class HouseholdSyncEngine
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
        private val room: QuedaDatabase,
        private val dao: SyncDao,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutex = Mutex()
        private var listener: ListenerRegistration? = null
        private var locationListener: ListenerRegistration? = null

        @Volatile
        private var activeId: String? = null
        private val _status = MutableStateFlow(HouseholdSyncStatus.PENDING)
        val status: StateFlow<HouseholdSyncStatus> = _status
        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error
        private val _conflictItem = MutableStateFlow<String?>(null)
        val conflictItem: StateFlow<String?> = _conflictItem

        fun start(householdId: String) {
            stop()
            activeId = householdId
            _status.value = HouseholdSyncStatus.PENDING
            listener =
                firestore.collection("households").document(householdId).collection("items")
                    .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                        if (activeId != householdId) return@addSnapshotListener
                        if (exception != null) {
                            _status.value = HouseholdSyncStatus.ERROR
                            _error.value = "No se pudo actualizar el hogar."
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            _status.value =
                                if (snapshot.metadata.isFromCache) HouseholdSyncStatus.OFFLINE else HouseholdSyncStatus.PENDING
                            scope.launch {
                                try {
                                    snapshot.documentChanges.forEach { change ->
                                        if (activeId == householdId) mergeRemote(householdId, change.document)
                                    }
                                    if (!snapshot.metadata.isFromCache) flush()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    fail(e)
                                }
                            }
                        }
                    }
            locationListener =
                firestore.collection("households").document(householdId).collection("locations")
                    .addSnapshotListener { snapshot, exception ->
                        if (activeId != householdId) return@addSnapshotListener
                        if (exception != null) {
                            fail(exception)
                        } else if (snapshot != null && !snapshot.metadata.isFromCache) {
                            flush()
                        }
                    }
            flush()
        }

        fun stop() {
            activeId = null
            listener?.remove()
            listener = null
            locationListener?.remove()
            locationListener = null
            _error.value = null
            _conflictItem.value = null
        }

        fun onLocalChange() {
            _status.value = HouseholdSyncStatus.PENDING
            flush()
        }

        fun refresh() {
            flush(fetchSnapshot = true)
        }

        fun discardConflict() {
            val householdId = activeId ?: return
            val itemId = _conflictItem.value ?: return
            scope.launch {
                mutex.withLock {
                    try {
                        val pending = dao.pending(householdId)
                        val batch = pending.firstOrNull { it.stockItemId == itemId }?.batchId.orEmpty()
                        val affected =
                            if (batch.isBlank()) {
                                listOf(
                                    itemId,
                                )
                            } else {
                                pending.filter { it.batchId == batch }.map { it.stockItemId }.distinct()
                            }
                        val remotes = affected.associateWith { itemRef(householdId, it).get(Source.SERVER).awaitTask() }
                        room.withTransaction {
                            affected.forEach { affectedId ->
                                val remote = checkNotNull(remotes[affectedId])
                                if (!remote.exists()) {
                                    val local = checkNotNull(dao.stockItem(affectedId))
                                    check(local.householdId == householdId)
                                    dao.deleteStockItem(affectedId)
                                    if (dao.stockItems(
                                            householdId,
                                        ).none { it.productId == local.productId }
                                    ) {
                                        dao.deleteProduct(local.productId)
                                    }
                                }
                                dao.discardPending(householdId, affectedId)
                            }
                        }
                        remotes.values.filter { it.exists() }.forEach { mergeRemote(householdId, it) }
                        _conflictItem.value = null
                        _error.value = null
                        _status.value = HouseholdSyncStatus.UPDATED
                        flush()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        fail(e)
                    }
                }
            }
        }

        private fun flush(fetchSnapshot: Boolean = false) {
            val householdId = activeId ?: return
            scope.launch {
                mutex.withLock {
                    if (activeId != householdId || auth.currentUser == null) return@withLock
                    try {
                        val locationConflict =
                            LocationSynchronizer(
                                firestore,
                                room,
                            ).flush(
                                householdId,
                                checkNotNull(auth.currentUser?.uid),
                            )
                        val operations = dao.pending(householdId)
                        val groups = operations.groupBy { it.batchId.ifBlank { it.id } }.values
                        for (group in groups) {
                            if (activeId != householdId) return@withLock
                            try {
                                uploadSuspend(group)
                            } catch (e: Exception) {
                                if (generateSequence(e as Throwable?) { it.cause }.any { it is IllegalStateException }) {
                                    _conflictItem.value = group.first().stockItemId
                                }
                                throw e
                            }
                            room.withTransaction { group.forEach { dao.acknowledge(it.id) } }
                            group.forEach { operation ->
                                val latest = itemRef(householdId, operation.stockItemId).get(Source.SERVER).awaitTask()
                                mergeRemote(householdId, latest)
                            }
                        }
                        // A cached listener event does not prove that the server was reached.
                        // Verify even an empty queue before reporting UPDATED.
                        if (activeId == householdId && (fetchSnapshot || operations.isEmpty())) {
                            val server =
                                firestore.collection("households").document(householdId)
                                    .collection("items").get(Source.SERVER).awaitTask()
                            server.documents.forEach { mergeRemote(householdId, it) }
                        }
                        if (activeId == householdId) {
                            _status.value = if (locationConflict) HouseholdSyncStatus.ERROR else HouseholdSyncStatus.UPDATED
                            _error.value =
                                if (locationConflict) {
                                    "Resuelve los cambios de ubicaciones en Más → Gestionar ubicaciones."
                                } else {
                                    null
                                }
                            _conflictItem.value = null
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (activeId == householdId) fail(e)
                    }
                }
            }
        }

        private suspend fun uploadSuspend(operations: List<PendingSyncOperationEntity>) {
            check(operations.size in 1..8)
            val uid = checkNotNull(auth.currentUser?.uid)
            val house = firestore.collection("households").document(operations.first().householdId)
            firestore.runTransaction { transaction ->
                // Every read precedes every write, including uniqueness claims for a new lot.
                val plans =
                    operations.map { op ->
                        val item = itemRef(op.householdId, op.stockItemId)
                        val receipt = item.collection("applied").document(op.id)
                        val payload = JSONObject(op.payload)
                        val added = op.action == "ADD" || op.action == "ADD_LOT"
                        val renamed = op.action == "RENAME"
                        val nameRef =
                            when {
                                added -> house.collection("names").document(sha(payload.getString("normalizedName")))
                                renamed -> house.collection("names").document(sha(payload.getString("desiredNormalizedName")))
                                else -> null
                            }
                        val oldNameRef =
                            if (renamed && payload.getString("expectedNormalizedName") != payload.getString("desiredNormalizedName")) {
                                house.collection("names").document(sha(payload.getString("expectedNormalizedName")))
                            } else {
                                null
                            }
                        val barcode = if (added) payload.nullableString("barcode") else null
                        val barcodeRef = barcode?.let { house.collection("barcodes").document(it) }
                        val applied = transaction.get(receipt)
                        val current = transaction.get(item)
                        val name = nameRef?.let { transaction.get(it) }
                        val oldName = oldNameRef?.let { transaction.get(it) }
                        val code = barcodeRef?.let { transaction.get(it) }
                        val anchor =
                            name?.getString("stockItemId")?.let {
                                transaction.get(
                                    itemRef(
                                        op.householdId,
                                        it,
                                    ),
                                )
                            }
                        UploadPlan(
                            op,
                            payload,
                            item,
                            receipt,
                            applied.exists(),
                            current,
                            nameRef,
                            name,
                            barcodeRef,
                            code,
                            anchor,
                            oldNameRef,
                            oldName,
                        )
                    }
                plans.filterNot { it.applied }.forEach { plan ->
                    if (plan.op.action == "ADD" || plan.op.action == "ADD_LOT") {
                        check(!plan.current.exists()) { "El alimento ya existe." }
                        if (plan.op.action == "ADD") {
                            check(plan.name?.exists() != true && plan.code?.exists() != true) {
                                "Nombre o código duplicado en el hogar."
                            }
                        }
                        if (plan.name?.exists() == true) {
                            check(plan.anchor?.getString("productId") == plan.payload.getString("productId")) {
                                "El producto cambió en otro dispositivo. Revisa el conflicto."
                            }
                        }
                        if (plan.code?.exists() == true) {
                            check(
                                plan.op.action == "ADD_LOT" &&
                                    plan.code.getString("stockItemId") == plan.name?.getString("stockItemId"),
                            ) { "Código duplicado." }
                        }
                        val keys =
                            listOf(
                                "productId", "displayName", "normalizedName", "barcode", "trackingMode",
                                "quantityAmount",
                                "quantityUnit",
                                "locationId",
                                "foodType",
                                "label",
                                "preparedOn",
                                "bestBefore",
                            )
                        val data =
                            keys.associateWith { plan.payload.nullableString(it) } +
                                mapOf(
                                    "foodType" to plan.payload.optString("foodType", "FOOD"),
                                    "isPresent" to if (plan.payload.isNull("isPresent")) null else plan.payload.getBoolean("isPresent"),
                                    "revision" to 1L, "updatedBy" to uid,
                                )
                        transaction.set(plan.item, data)
                        if (plan.name?.exists() != true) {
                            plan.nameRef?.let {
                                transaction.set(
                                    it,
                                    mapOf("stockItemId" to plan.op.stockItemId),
                                )
                            }
                        }
                        if (plan.code?.exists() != true) {
                            plan.codeRef?.let {
                                transaction.set(
                                    it,
                                    mapOf("stockItemId" to plan.op.stockItemId),
                                )
                            }
                        }
                    } else {
                        check(plan.current.exists()) { "El alimento ya no existe." }
                        if (plan.op.action == "RENAME") {
                            check(
                                plan.name?.exists() != true ||
                                    plan.anchor?.getString("productId") == plan.payload.getString("productId"),
                            ) { "Nombre duplicado en el hogar." }
                            check(plan.oldName == null || plan.oldName.exists()) {
                                "La reserva del nombre cambió. Vuelve a revisarlo."
                            }
                        }
                        val changes =
                            when (plan.op.action) {
                                "DETAILS" -> RemoteStockMutations.details(plan.current, plan.payload)
                                "CONSUME_ALL" -> RemoteStockMutations.consumeAll(plan.current, plan.payload)
                                "RENAME" -> RemoteStockMutations.rename(plan.current, plan.payload)
                                else -> RemoteStockMutations.mutate(plan.current, plan.op.action, plan.payload)
                            }
                        transaction.update(
                            plan.item,
                            changes +
                                mapOf(
                                    "revision" to checkNotNull(plan.current.getLong("revision")) + 1,
                                    "updatedBy" to uid,
                                ),
                        )
                        if (plan.op.action == "RENAME" &&
                            plan.op.id == plans.first { it.op.action == "RENAME" }.op.id
                        ) {
                            if (plan.name?.exists() != true) {
                                transaction.set(checkNotNull(plan.nameRef), mapOf("stockItemId" to plan.op.stockItemId))
                            }
                            plan.oldNameRef?.let { transaction.delete(it) }
                        }
                    }
                    transaction.set(plan.receipt, mapOf("uid" to uid))
                }
                Unit
            }.awaitTask()
        }

        private data class UploadPlan(
            val op: PendingSyncOperationEntity,
            val payload: JSONObject,
            val item: DocumentReference,
            val receipt: DocumentReference,
            val applied: Boolean,
            val current: DocumentSnapshot,
            val nameRef: DocumentReference?,
            val name: DocumentSnapshot?,
            val codeRef: DocumentReference?,
            val code: DocumentSnapshot?,
            val anchor: DocumentSnapshot?,
            val oldNameRef: DocumentReference?,
            val oldName: DocumentSnapshot?,
        )

        private suspend fun mergeRemote(
            householdId: String,
            remote: DocumentSnapshot,
        ) {
            if (!remote.exists() || activeId != householdId) return
            room.withTransaction {
                if (dao.pendingForItem(householdId, remote.id) > 0) return@withTransaction
                val productId = checkNotNull(remote.getString("productId"))
                val product =
                    ProductEntity(
                        productId,
                        householdId,
                        checkNotNull(remote.getString("displayName")),
                        checkNotNull(remote.getString("normalizedName")),
                        remote.getString("barcode"),
                    )
                val item =
                    StockItemEntity(
                        remote.id,
                        householdId,
                        productId,
                        checkNotNull(remote.getString("trackingMode")),
                        remote.getString("quantityAmount"),
                        remote.getString("quantityUnit"),
                        remote.getBoolean("isPresent"),
                        remote.getString("locationId"),
                        remote.getString("foodType") ?: "FOOD",
                        remote.getString("label"),
                        remote.getString("preparedOn"), remote.getString("bestBefore"),
                    )
                val localProduct = dao.product(productId)
                if (localProduct == null) {
                    dao.insertProduct(product)
                } else {
                    check(localProduct.householdId == householdId)
                    if (localProduct != product) dao.updateProduct(product)
                }
                val localItem = dao.stockItem(item.id)
                if (localItem == null) {
                    dao.insertStockItem(item)
                } else {
                    check(localItem.householdId == householdId && localItem.productId == productId)
                    room.managementDao().updateStock(item)
                }
            }
        }

        private fun itemRef(
            householdId: String,
            id: String,
        ): DocumentReference = firestore.collection("households").document(householdId).collection("items").document(id)

        private fun sha(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }

        private fun fail(error: Exception) {
            _status.value =
                if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    HouseholdSyncStatus.OFFLINE
                } else {
                    HouseholdSyncStatus.ERROR
                }
            _error.value = error.localizedMessage ?: "La sincronización no se completó."
        }
    }
