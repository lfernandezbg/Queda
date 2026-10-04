package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.ReceiptDraftEntity
import com.luisete.queda.core.model.inventory.ReceiptDraft
import com.luisete.queda.core.model.inventory.ReceiptLine
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.json.JSONArray
import org.json.JSONObject

internal object ReceiptDraftCodec {
    fun encode(draft: ReceiptDraft): String =
        JSONArray().apply {
            draft.lines.forEach { line ->
                put(
                    JSONObject().apply {
                        put("id", line.id)
                        put("source", line.source)
                        put("name", line.name)
                        put("quantity", line.quantity)
                        put("unit", line.unit.name)
                        put("selected", line.selected)
                        put("reviewed", line.reviewed)
                        put("locationId", line.locationId ?: JSONObject.NULL)
                    },
                )
            }
        }.toString()

    fun decode(entity: ReceiptDraftEntity): ReceiptDraft {
        val json = JSONArray(entity.payload)
        val lines =
            (0 until json.length()).map { index ->
                val line = json.getJSONObject(index)
                ReceiptLine(
                    line.getString("id"),
                    line.getString("source"),
                    line.getString("name"),
                    line.getString("quantity"),
                    MeasurementUnit.valueOf(line.getString("unit")),
                    line.getBoolean("selected"),
                    line.getBoolean("reviewed"),
                    if (line.isNull("locationId")) null else line.getString("locationId"),
                )
            }
        return ReceiptDraft(entity.id, entity.fingerprint, lines)
    }
}
