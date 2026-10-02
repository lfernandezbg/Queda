@file:Suppress(
    "detekt:TooManyFunctions",
    "detekt:TooGenericExceptionCaught",
    "detekt:ReturnCount",
    "detekt:MagicNumber",
    "detekt:MaxLineLength",
)

package com.luisete.queda.core.data.household

import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.SyncDao
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.model.id.HouseholdId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

sealed interface HouseholdSession {
    data object SignedOut : HouseholdSession

    data object Loading : HouseholdSession

    data object ChooseHousehold : HouseholdSession

    data object LoadFailed : HouseholdSession

    data class Active(val id: String, val name: String) : HouseholdSession
}

@Singleton
class FirebaseHouseholdManager
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
        private val database: QuedaDatabase,
        private val syncDao: SyncDao,
        private val sync: HouseholdSyncEngine,
        private val shoppingSync: ShoppingSyncEngine,
    ) : CurrentHouseholdIdProvider {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val generation = AtomicInteger(0)
        private val _state = MutableStateFlow<HouseholdSession>(HouseholdSession.Loading)
        val state: StateFlow<HouseholdSession> = _state
        private val _message = MutableStateFlow<String?>(null)
        val message: StateFlow<String?> = _message
        private val _inviteCode = MutableStateFlow<String?>(null)
        val inviteCode: StateFlow<String?> = _inviteCode
        val syncStatus: StateFlow<HouseholdSyncStatus> =
            combine(sync.status, shoppingSync.status) { inventory, shopping ->
                when {
                    inventory == HouseholdSyncStatus.ERROR || shopping == HouseholdSyncStatus.ERROR -> HouseholdSyncStatus.ERROR
                    inventory == HouseholdSyncStatus.OFFLINE || shopping == HouseholdSyncStatus.OFFLINE -> HouseholdSyncStatus.OFFLINE
                    inventory == HouseholdSyncStatus.PENDING || shopping == HouseholdSyncStatus.PENDING -> HouseholdSyncStatus.PENDING
                    else -> HouseholdSyncStatus.UPDATED
                }
            }.stateIn(scope, SharingStarted.Eagerly, HouseholdSyncStatus.PENDING)
        val syncError: StateFlow<String?> = sync.error
        val conflictItem: StateFlow<String?> = sync.conflictItem

        fun refresh() {
            sync.refresh()
            shoppingSync.refresh()
        }

        fun discardConflict() = sync.discardConflict()

        fun retry() {
            scope.launch { loadHousehold() }
        }

        override fun currentHouseholdId(): HouseholdId =
            HouseholdId.from((state.value as? HouseholdSession.Active)?.id ?: "local-household-v1")

        fun start() {
            if (auth.currentUser == null) {
                _state.value = HouseholdSession.SignedOut
            } else {
                _state.value = HouseholdSession.Loading
                scope.launch { loadHousehold() }
            }
        }

        fun register(
            email: String,
            password: String,
        ) = launchAction {
            auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
            loadHousehold()
        }

        fun signIn(
            email: String,
            password: String,
        ) = launchAction {
            auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
            loadHousehold()
        }

        fun signOut() {
            generation.incrementAndGet()
            sync.stop()
            shoppingSync.stop()
            auth.signOut()
            _message.value = null
            _inviteCode.value = null
            _state.value = HouseholdSession.SignedOut
        }

        fun createHousehold(name: String) =
            launchAction {
                val uid = checkNotNull(auth.currentUser?.uid)
                require(name.trim().length in 2..60) { "El nombre del hogar debe tener entre 2 y 60 caracteres." }
                val id = HouseholdId.newId().value
                val household = firestore.collection("households").document(id)
                val member = household.collection("members").document(uid)
                val profile = firestore.collection("users").document(uid)
                firestore.batch()
                    .set(household, mapOf("ownerUid" to uid, "name" to name.trim(), "createdAt" to Timestamp.now()))
                    .set(member, mapOf("joinedAt" to Timestamp.now()))
                    .set(profile, mapOf("householdId" to id))
                    .commit().awaitTask()
                check(auth.currentUser?.uid == uid) { "Sesión cerrada durante la creación del hogar." }
                readWithOfflineFallback(profile)
                readWithOfflineFallback(household)
                database.withTransaction { syncDao.importLegacy(id) }
                check(auth.currentUser?.uid == uid) { "La cuenta cambió durante la creación del hogar." }
                enter(id, name.trim())
            }

        fun joinHousehold(code: String) =
            launchAction {
                val uid = checkNotNull(auth.currentUser?.uid)
                require(code.matches(Regex("[a-f0-9]{32}"))) { "El código de invitación no es válido." }
                val invite = firestore.collection("invites").document(code).get(com.google.firebase.firestore.Source.SERVER).awaitTask()
                val id = checkNotNull(invite.getString("householdId")) { "Invitación no encontrada." }
                val household = firestore.collection("households").document(id)
                val member = household.collection("members").document(uid)
                val profile = firestore.collection("users").document(uid)
                firestore.batch()
                    .set(member, mapOf("joinedAt" to Timestamp.now(), "inviteCode" to code))
                    .set(profile, mapOf("householdId" to id))
                    .commit().awaitTask()
                check(auth.currentUser?.uid == uid) { "Sesión cerrada durante la unión al hogar." }
                readWithOfflineFallback(profile)
                val name = readWithOfflineFallback(household).getString("name") ?: "Hogar"
                database.withTransaction { syncDao.importLegacy(id) }
                check(auth.currentUser?.uid == uid) { "La cuenta cambió durante la unión al hogar." }
                enter(id, name)
            }

        fun createInvite() =
            launchAction {
                val uid = checkNotNull(auth.currentUser?.uid)
                val id = checkNotNull((state.value as? HouseholdSession.Active)?.id)
                val bytes = ByteArray(16)
                SecureRandom().nextBytes(bytes)
                val code = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
                val expiry = Timestamp(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
                firestore.collection("invites").document(code)
                    .set(mapOf("householdId" to id, "createdBy" to uid, "expiresAt" to expiry)).awaitTask()
                check(auth.currentUser?.uid == uid) { "La cuenta cambió durante la invitación." }
                _inviteCode.value = code
            }

        fun clearMessage() {
            _message.value = null
        }

        private fun launchAction(action: suspend () -> Unit) {
            _message.value = null
            val started = generation.get()
            scope.launch {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (generation.get() == started) {
                        _message.value = e.localizedMessage ?: "No se pudo completar la operación."
                    }
                }
            }
        }

        private suspend fun loadHousehold() {
            val uid =
                auth.currentUser?.uid ?: run {
                    _state.value = HouseholdSession.SignedOut
                    return
                }
            _state.value = HouseholdSession.Loading
            try {
                val profile = readWithOfflineFallback(firestore.collection("users").document(uid))
                if (auth.currentUser?.uid != uid) return
                val id = profile.getString("householdId")
                if (id == null) {
                    _state.value = HouseholdSession.ChooseHousehold
                    return
                }
                val household = readWithOfflineFallback(firestore.collection("households").document(id))
                if (auth.currentUser?.uid != uid) return
                val name = checkNotNull(household.getString("name")) { "No tienes acceso al hogar." }
                database.withTransaction { syncDao.importLegacy(id) }
                if (auth.currentUser?.uid != uid) return
                enter(id, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (auth.currentUser?.uid != uid) return
                _message.value = e.localizedMessage ?: "No se pudo cargar el hogar."
                _state.value = HouseholdSession.LoadFailed
            }
        }

        private fun enter(
            id: String,
            name: String,
        ) {
            _state.value = HouseholdSession.Active(id, name)
            sync.start(id)
            shoppingSync.start(id)
        }

        private suspend fun readWithOfflineFallback(
            ref: com.google.firebase.firestore.DocumentReference,
        ): com.google.firebase.firestore.DocumentSnapshot =
            try {
                ref.get(com.google.firebase.firestore.Source.SERVER).awaitTask()
            } catch (_: Exception) {
                ref.get(com.google.firebase.firestore.Source.CACHE).awaitTask()
            }
    }

internal suspend fun <T> Task<T>.awaitTask(): T =
    withContext(Dispatchers.IO) {
        com.google.android.gms.tasks.Tasks.await(this@awaitTask)
    }
