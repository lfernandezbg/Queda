package com.luisete.queda.core.model.inventory

import com.luisete.queda.core.model.quantity.MeasurementUnit

data class ReceiptLine(
    val id: String,
    val source: String,
    val name: String,
    val quantity: String = "1",
    val unit: MeasurementUnit = MeasurementUnit.UNIT,
    val selected: Boolean = false,
    val reviewed: Boolean = false,
    val locationId: String? = null,
)

data class ReceiptDraft(val id: String, val fingerprint: String, val lines: List<ReceiptLine>)
