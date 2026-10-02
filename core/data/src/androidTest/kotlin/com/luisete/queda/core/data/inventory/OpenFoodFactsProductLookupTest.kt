package com.luisete.queda.core.data.inventory

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luisete.queda.core.domain.inventory.ExternalProductResult
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
class OpenFoodFactsProductLookupTest {
    private val barcode = (Barcode.create("4006381333931") as BarcodeCreationResult.Success).barcode

    @Test
    fun spanishNameWinsAndRequestUsesScopedProductEndpoint() =
        runTest {
            lateinit var connection: FakeConnection
            val lookup =
                OpenFoodFactsProductLookup { url ->
                    connection =
                        FakeConnection(
                            url,
                            200,
                            """{"status":1,"product":{"product_name":"Milk","product_name_es":"Leche entera"}}""",
                        )
                    connection
                }

            assertEquals(ExternalProductResult.Found("Leche entera"), lookup.findByBarcode(barcode))
            assertEquals("/api/v2/product/4006381333931.json", connection.url.path)
            assertTrue(connection.url.query.contains("fields=product_name_es,product_name"))
            assertTrue(connection.getRequestProperty("User-Agent").startsWith("Queda/"))
            assertEquals(4000, connection.connectTimeout)
            assertEquals(4000, connection.readTimeout)
            assertTrue(connection.disconnected)
        }

    @Test
    fun englishNameIsUsedWhenSpanishNameIsAbsent() =
        runTest {
            val lookup =
                OpenFoodFactsProductLookup { url ->
                    FakeConnection(url, 200, """{"status":1,"product":{"product_name":"Milk"}}""")
                }
            assertEquals(ExternalProductResult.Found("Milk"), lookup.findByBarcode(barcode))
        }

    @Test
    fun missingNameAndMissingProductAreDifferent() =
        runTest {
            val unnamed =
                OpenFoodFactsProductLookup { url ->
                    FakeConnection(url, 200, """{"status":1,"product":{}}""")
                }
            val unknown =
                OpenFoodFactsProductLookup { url ->
                    FakeConnection(url, 200, """{"status":0}""")
                }
            assertEquals(ExternalProductResult.MissingName, unnamed.findByBarcode(barcode))
            assertEquals(ExternalProductResult.NotFound, unknown.findByBarcode(barcode))
        }

    @Test
    fun malformedResponseAndNetworkFailureAllowManualFallback() =
        runTest {
            val malformed = OpenFoodFactsProductLookup { url -> FakeConnection(url, 200, "{") }
            val offline =
                OpenFoodFactsProductLookup { _ ->
                    throw IOException("No network")
                }
            assertEquals(ExternalProductResult.Unavailable, malformed.findByBarcode(barcode))
            assertEquals(ExternalProductResult.Unavailable, offline.findByBarcode(barcode))
        }

    private class FakeConnection(
        url: URL,
        private val status: Int,
        private val body: String,
    ) : HttpURLConnection(url) {
        var disconnected = false

        override fun connect() = Unit

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int = status

        override fun getInputStream() = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    }
}
