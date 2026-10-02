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
import com.google.firebase.firestore.Source
import com.luisete.queda.core.database.PendingSyncOperationEntity
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.database.SyncDao
import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.domain.result.Failure
import com.luisete.queda.core.domain.result.Success
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
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
                    .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, exception ->
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
            flush()
        }

        fun stop() {
            activeId = null
            listener?.remove()
            listener = null
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
                        val remote = itemRef(householdId, itemId).get(Source.SERVER).awaitTask()
                        room.withTransaction {
                            if (!remote.exists()) {
                                val local = checkNotNull(dao.stockItem(itemId))
                                check(local.householdId == householdId)
                                dao.deleteStockItem(itemId)
                                dao.deleteProduct(local.productId)
                            }
                            dao.discardPending(householdId, itemId)
                        }
                        if (remote.exists()) {
                            mergeRemote(householdId, remote)
                        }
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
                        val operations = dao.pending(householdId)
                        for (operation in operations) {
                            if (activeId != householdId) return@withLock
                            try {
                                uploadSuspend(operation)
                            } catch (e: Exception) {
                                if (generateSequence(e as Throwable?) { it.cause }.any { it is IllegalStateException }) {
                                    _conflictItem.value = operation.stockItemId
                                }
                                throw e
                            }
                            dao.acknowledge(operation.id)
                            val latest = itemRef(householdId, operation.stockItemId).get(Source.SERVER).awaitTask()
                            mergeRemote(householdId, latest)
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
                            _status.value = HouseholdSyncStatus.UPDATED
                            _error.value = null
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

        private suspend fun uploadSuspend(op: PendingSyncOperationEntity) {
            // Firestore transactions serialize concurrent quantity operations at the server.
            // Each operation has an immutable receipt so retrying after an interrupted ACK is safe.
            val item = itemRef(op.householdId, op.stockItemId)
            val receipt = item.collection("applied").document(op.id)
            val payload = JSONObject(op.payload)
            val uid = checkNotNull(auth.currentUser?.uid)
            val house = firestore.collection("households").document(op.householdId)
            val nameClaim =
                if (op.action == "ADD") house.collection("names").document(sha(payload.getString("normalizedName"))) else null
            val barcode =
                if (op.action == "ADD") payload.optString("barcode").takeIf { it.isNotBlank() && it != "null" } else null
            val barcodeClaim = barcode?.let { house.collection("barcodes").document(it) }
            firestore.runTransaction { transaction ->
                val applied = transaction.get(receipt)
                val current = transaction.get(item)
                val name = nameClaim?.let { transaction.get(it) }
                val code = barcodeClaim?.let { transaction.get(it) }
                if (applied.exists()) return@runTransaction Unit
                if (op.action == "ADD") {
                    check(!current.exists()) { "El producto ya existe; revisa el conflicto antes de sincronizar." }
                    check(name?.exists() != true && code?.exists() != true) { "Nombre o código duplicado en el hogar." }
                    transaction.set(
                        item,
                        mapOf(
                            "productId" to payload.getString("productId"),
                            "displayName" to payload.getString("displayName"),
                            "normalizedName" to payload.getString("normalizedName"),
                            "barcode" to barcode,
                            "trackingMode" to payload.getString("trackingMode"),
                            "quantityAmount" to payload.optString("quantityAmount").takeIf { it.isNotBlank() && it != "null" },
                            "quantityUnit" to payload.optString("quantityUnit").takeIf { it.isNotBlank() && it != "null" },
                            "isPresent" to if (payload.isNull("isPresent")) null else payload.getBoolean("isPresent"),
                            "revision" to 1L,
                            "updatedBy" to uid,
                        ),
                    )
                    nameClaim?.let { transaction.set(it, mapOf("stockItemId" to op.stockItemId)) }
                    barcodeClaim?.let { transaction.set(it, mapOf("stockItemId" to op.stockItemId)) }
                } else {
                    check(current.exists()) { "El alimento ya no existe en el hogar." }
                    val changes =
                        when (op.action) {
                            "PRESENCE" -> {
                                check(current.getString("trackingMode") == "PRESENCE")
                                mapOf("isPresent" to payload.getBoolean("isPresent"))
                            }
                            "CORRECT" -> {
                                check(current.getString("trackingMode") == "EXACT")
                                val desired =
                                    ExactQuantity.of(
                                        payload.getString("quantityAmount"),
                                        MeasurementUnit.valueOf(payload.getString("quantityUnit")),
                                    )
                                val previous = remoteQuantity(current)
                                val result = QuantityOperations.correct(previous, desired.amount, desired.unit)
                                val corrected =
                                    when (result) {
                                        is Success -> result.value
                                        is Failure ->
                                            if (result.error == DomainError.UnchangedQuantity) {
                                                previous
                                            } else {
                                                error("La corrección ya no es válida; revisa la cantidad.")
                                            }
                                    }
                                mapOf("quantityAmount" to corrected.amount.toPlainString(), "quantityUnit" to corrected.unit.name)
                            }
                            "CONSUME" -> {
                                check(current.getString("trackingMode") == "EXACT")
                                val delta =
                                    ExactQuantity.of(
                                        payload.getString("quantityAmount"),
                                        MeasurementUnit.valueOf(payload.getString("quantityUnit")),
                                    )
                                val result = QuantityOperations.consume(remoteQuantity(current), delta)
                                check(result is Success) { "No queda cantidad suficiente tras los cambios de otra persona." }
                                mapOf("quantityAmount" to result.value.amount.toPlainString(), "quantityUnit" to result.value.unit.name)
                            }
                            "ADD_QUANTITY" -> {
                                check(current.getString("trackingMode") == "EXACT")
                                val delta =
                                    ExactQuantity.of(
                                        payload.getString("quantityAmount"),
                                        MeasurementUnit.valueOf(payload.getString("quantityUnit")),
                                    )
                                check(delta.amount.signum() > 0)
                                val result = QuantityOperations.add(remoteQuantity(current), delta)
                                check(result is Success) { "La unidad ya no es compatible con este alimento." }
                                mapOf("quantityAmount" to result.value.amount.toPlainString(), "quantityUnit" to result.value.unit.name)
                            }
                            else -> error("Operación desconocida")
                        }
                    transaction.update(
                        item,
                        changes + mapOf("revision" to (checkNotNull(current.getLong("revision")) + 1), "updatedBy" to uid),
                    )
                }
                transaction.set(receipt, mapOf("uid" to uid))
                Unit
            }.awaitTask()
        }

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
                    )
                val localProduct = dao.product(productId)
                if (localProduct == null) {
                    dao.insertProduct(product)
                } else {
                    check(localProduct.householdId == householdId && localProduct.normalizedName == product.normalizedName)
                }
                val localItem = dao.stockItem(item.id)
                if (localItem == null) {
                    dao.insertStockItem(item)
                } else {
                    check(localItem.householdId == householdId && localItem.productId == productId)
                    dao.setRemoteStock(item.id, householdId, item.quantityAmount, item.quantityUnit, item.isPresent)
                }
            }
        }

        private fun remoteQuantity(doc: DocumentSnapshot): ExactQuantity =
            ExactQuantity.of(
                checkNotNull(doc.getString("quantityAmount")),
                MeasurementUnit.valueOf(checkNotNull(doc.getString("quantityUnit"))),
            )

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
