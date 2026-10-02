@file:Suppress("detekt:SwallowedException", "detekt:ReturnCount", "detekt:MagicNumber")

package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.domain.inventory.ExternalProductLookup
import com.luisete.queda.core.domain.inventory.ExternalProductResult
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class OpenFoodFactsProductLookup internal constructor(
    private val connectionFactory: (URL) -> HttpURLConnection,
) : ExternalProductLookup {
    @Inject
    constructor() : this({ url -> url.openConnection() as HttpURLConnection })

    override suspend fun findByBarcode(barcode: Barcode): ExternalProductResult =
        withContext(Dispatchers.IO) {
            val url =
                URL(
                    "https://world.openfoodfacts.org/api/v2/product/${barcode.value}.json" +
                        "?fields=product_name_es,product_name",
                )
            var connection: HttpURLConnection? = null
            try {
                val activeConnection = connectionFactory(url)
                connection = activeConnection
                activeConnection.connectTimeout = 4000
                activeConnection.readTimeout = 4000
                activeConnection.setRequestProperty("User-Agent", "Queda/1.0 (Android; com.luisete.queda)")
                when (activeConnection.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        activeConnection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                            parseResponse(reader.readText())
                        }

                    HttpURLConnection.HTTP_NOT_FOUND -> ExternalProductResult.NotFound
                    else -> ExternalProductResult.Unavailable
                }
            } catch (e: IOException) {
                ExternalProductResult.Unavailable
            } catch (e: JSONException) {
                ExternalProductResult.Unavailable
            } finally {
                connection?.disconnect()
            }
        }

    internal fun parseResponse(json: String): ExternalProductResult {
        val response = JSONObject(json)
        if (response.optInt("status", -1) == 0) return ExternalProductResult.NotFound
        if (response.optInt("status", -1) != 1) return ExternalProductResult.Unavailable

        val product = response.optJSONObject("product") ?: return ExternalProductResult.MissingName
        val candidates = listOf(product.optString("product_name_es"), product.optString("product_name"))
        val name =
            candidates.firstNotNullOfOrNull { candidate ->
                when (val result = ProductName.create(candidate)) {
                    is ProductNameCreationResult.Success -> result.productName.displayValue
                    else -> null
                }
            }
        return if (name == null) ExternalProductResult.MissingName else ExternalProductResult.Found(name)
    }
}
