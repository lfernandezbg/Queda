package com.luisete.queda.core.database

import org.json.JSONObject

object SyncPayloadCodec {
    fun encode(
        product: ProductEntity,
        item: StockItemEntity,
    ): String =
        JSONObject().apply {
            put("productId", product.id)
            put("displayName", product.displayName)
            put("normalizedName", product.normalizedName)
            put("barcode", product.barcode ?: JSONObject.NULL)
            put("trackingMode", item.trackingMode)
            put("quantityAmount", item.quantityAmount ?: JSONObject.NULL)
            put("quantityUnit", item.quantityUnit ?: JSONObject.NULL)
            put("isPresent", item.isPresent ?: JSONObject.NULL)
        }.toString()

    fun quantity(
        amount: String,
        unit: String,
    ): String = JSONObject().put("quantityAmount", amount).put("quantityUnit", unit).toString()

    fun presence(isPresent: Boolean): String = JSONObject().put("isPresent", isPresent).toString()
}
