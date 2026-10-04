package com.luisete.queda.core.data.household

import com.google.firebase.firestore.DocumentSnapshot
import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.domain.result.Failure
import com.luisete.queda.core.domain.result.Success
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.json.JSONObject

internal object RemoteStockMutations {
    fun rename(
        current: DocumentSnapshot,
        payload: JSONObject,
    ): Map<String, Any?> {
        check(
            current.getString("productId") == payload.getString("productId") &&
                current.getString("displayName") == payload.getString("expectedName") &&
                current.getString("normalizedName") == payload.getString("expectedNormalizedName"),
        ) { "El nombre cambió en otro dispositivo. Vuelve a revisarlo." }
        val metadata =
            if (payload.has("expected")) details(current, payload) else emptyMap()
        return metadata +
            mapOf(
                "displayName" to payload.getString("desiredName"),
                "normalizedName" to payload.getString("desiredNormalizedName"),
            )
    }

    fun details(
        current: DocumentSnapshot,
        payload: JSONObject,
    ): Map<String, Any?> {
        val expected = payload.getJSONObject("expected")
        val desired = payload.getJSONObject("desired")
        val keys = listOf("locationId", "foodType", "label", "preparedOn", "bestBefore")
        check(
            keys.all { key ->
                val actual = current.getString(key) ?: if (key == "foodType") "FOOD" else null
                actual == expected.nullableString(key)
            },
        ) { "La ubicación o los datos del alimento cambiaron. Vuelve a revisarlos." }
        return keys.associateWith { desired.nullableString(it) }
    }

    fun consumeAll(
        current: DocumentSnapshot,
        payload: JSONObject,
    ): Map<String, Any?> {
        return if (current.getString("trackingMode") == "EXACT") {
            val expected =
                ExactQuantity.of(
                    payload.getString("expectedAmount"),
                    MeasurementUnit.valueOf(payload.getString("expectedUnit")),
                )
            val actual = quantity(current)
            check(
                actual == expected &&
                    actual.amount.signum() > 0,
            ) { "La cantidad cambió. Vuelve a seleccionar los alimentos." }
            val requested = payload.optString("consumeAmount").takeIf(String::isNotBlank)
            val result =
                if (requested == null) {
                    ExactQuantity.of("0", actual.unit)
                } else {
                    val unit = MeasurementUnit.valueOf(payload.getString("consumeUnit"))
                    when (val consumed = QuantityOperations.consume(actual, ExactQuantity.of(requested, unit))) {
                        is Success -> consumed.value
                        is Failure -> error("La cantidad ya no permite esta operación.")
                    }
                }
            mapOf("quantityAmount" to result.amount.toPlainString(), "quantityUnit" to result.unit.name)
        } else {
            check(
                current.getString("trackingMode") == "PRESENCE" && current.getBoolean("isPresent") == true &&
                    payload.getBoolean("expectedPresence"),
            ) { "La presencia cambió. Vuelve a seleccionar los alimentos." }
            mapOf("isPresent" to false)
        }
    }

    fun quantity(current: DocumentSnapshot): ExactQuantity =
        ExactQuantity.of(
            checkNotNull(current.getString("quantityAmount")),
            MeasurementUnit.valueOf(checkNotNull(current.getString("quantityUnit"))),
        )

    fun mutate(
        current: DocumentSnapshot,
        action: String,
        payload: JSONObject,
    ): Map<String, Any?> {
        if (action == "PRESENCE") {
            check(current.getString("trackingMode") == "PRESENCE")
            return mapOf("isPresent" to payload.getBoolean("isPresent"))
        }
        check(current.getString("trackingMode") == "EXACT")
        val input =
            ExactQuantity.of(
                payload.getString("quantityAmount"),
                MeasurementUnit.valueOf(payload.getString("quantityUnit")),
            )
        val previous = quantity(current)
        val result =
            when (action) {
                "CONSUME" -> QuantityOperations.consume(previous, input)
                "ADD_QUANTITY" -> QuantityOperations.add(previous, input)
                "CORRECT" -> QuantityOperations.correct(previous, input.amount, input.unit)
                else -> error("Operación desconocida")
            }
        val next =
            when (result) {
                is Success -> result.value
                is Failure ->
                    if (action == "CORRECT" && previous == input) {
                        previous
                    } else {
                        error("La cantidad ya no permite esta operación. Revisa el alimento.")
                    }
            }
        return mapOf("quantityAmount" to next.amount.toPlainString(), "quantityUnit" to next.unit.name)
    }
}

internal fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
