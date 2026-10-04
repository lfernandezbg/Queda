package com.luisete.queda.core.data.household

import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.luisete.queda.core.database.LocationEntity
import com.luisete.queda.core.database.QuedaDatabase
import org.json.JSONObject
import java.security.MessageDigest

internal class LocationSynchronizer(private val firestore: FirebaseFirestore, private val room: QuedaDatabase) {
    suspend fun flush(
        hid: String,
        uid: String,
    ): Boolean {
        val dao = room.managementDao()
        val house = firestore.collection("households").document(hid)
        for (local in dao.pendingLocations(hid).filter { it.conflictPayload == null }) {
            val conflict = upload(local, uid)
            room.withTransaction {
                // Edits made during upload stay pending and use the newly acknowledged revision.
                val current = dao.location(hid, local.id)
                if (current != null) {
                    dao.saveLocation(
                        if (conflict != null) {
                            current.copy(conflictPayload = conflict)
                        } else {
                            current.copy(
                                revision = local.revision + 1,
                                syncPending = current.name != local.name || current.archived != local.archived,
                            )
                        },
                    )
                }
            }
        }
        val remote = house.collection("locations").get(Source.SERVER).awaitTask()
        room.withTransaction {
            remote.documents.forEach { doc ->
                val current = dao.location(hid, doc.id)
                if (current?.syncPending != true) {
                    dao.saveLocation(
                        LocationEntity(
                            doc.id,
                            hid,
                            checkNotNull(doc.getString("name")),
                            checkNotNull(doc.getString("normalizedName")),
                            checkNotNull(doc.getBoolean("archived")),
                            checkNotNull(doc.getLong("revision")),
                            false,
                        ),
                    )
                }
            }
        }
        return dao.pendingLocations(hid).any { it.conflictPayload != null }
    }

    private suspend fun upload(
        local: LocationEntity,
        uid: String,
    ): String? {
        val house = firestore.collection("households").document(local.householdId)
        val ref = house.collection("locations").document(local.id)
        val claim = house.collection("locationNames").document(hash(local.normalizedName))
        return firestore.runTransaction { transaction ->
            val remote = transaction.get(ref)
            val claimed = transaction.get(claim)
            val oldName = remote.getString("normalizedName")
            val oldClaim =
                oldName?.takeIf { it != local.normalizedName }?.let {
                    house.collection("locationNames").document(hash(it))
                }
            if (oldClaim != null) transaction.get(oldClaim)
            val alreadyApplied =
                remote.getLong("revision") == local.revision + 1 &&
                    remote.getString("name") == local.name && remote.getBoolean("archived") == local.archived
            if (!alreadyApplied) {
                if ((remote.getLong("revision") ?: 0) != local.revision) {
                    return@runTransaction JSONObject()
                        .put("name", checkNotNull(remote.getString("name")))
                        .put("normalizedName", checkNotNull(remote.getString("normalizedName")))
                        .put("archived", checkNotNull(remote.getBoolean("archived")))
                        .put("revision", checkNotNull(remote.getLong("revision"))).toString()
                }
                check(
                    !claimed.exists() ||
                        claimed.getString("locationId") == local.id,
                ) { "Ya existe esa ubicación." }
                transaction.set(
                    ref,
                    mapOf(
                        "name" to local.name,
                        "normalizedName" to local.normalizedName,
                        "archived" to local.archived,
                        "revision" to local.revision + 1,
                        "updatedBy" to uid,
                    ),
                )
                transaction.set(claim, mapOf("locationId" to local.id))
                if (oldClaim != null) transaction.delete(oldClaim)
            }
            null
        }.awaitTask()
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and HEX_MASK) }

    companion object {
        private const val HEX_MASK = 0xff
    }
}
