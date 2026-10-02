package com.luisete.queda.core.data.inventory

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luisete.queda.core.domain.inventory.ExternalProductResult
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenFoodFactsLiveApiSupervisedTest {
    @Test
    fun testLiveApiSupervised() =
        runBlocking {
            val lookup = OpenFoodFactsProductLookup()
            val barcode = (Barcode.create("3017620422003") as BarcodeCreationResult.Success).barcode
            val result = lookup.findByBarcode(barcode)
            println("LIVE_OFF_SUPERVISED_TEST: Result for 3017620422003 = $result")
            assertTrue("Expected Found but got $result", result is ExternalProductResult.Found)
            val found = result as ExternalProductResult.Found
            assertTrue("Expected non-blank name but got '${found.name}'", found.name.isNotBlank())
        }

    @Test
    fun testLiveApiNonExistentBarcode() =
        runBlocking {
            val lookup = OpenFoodFactsProductLookup()
            val barcode = (Barcode.create("0000000000000") as BarcodeCreationResult.Success).barcode
            val result = lookup.findByBarcode(barcode)
            println("LIVE_OFF_SUPERVISED_TEST: Result for 0000000000000 = $result")
            assertEquals(ExternalProductResult.NotFound, result)
        }
}
