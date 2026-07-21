package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeInventoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ResolveScannedBarcodeUseCaseTest {
    private lateinit var repository: FakeInventoryRepository
    private lateinit var useCase: ResolveScannedBarcodeUseCase

    @Before
    fun setup() {
        repository = FakeInventoryRepository()
        useCase = ResolveScannedBarcodeUseCase(repository)
    }

    @Test
    fun `blank input returns BLANK error and does not call repository`() =
        runTest {
            val result = useCase("")
            assertEquals(ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.BLANK), result)
            assertEquals(0, repository.findItemByBarcodeCalls.size)
        }

    @Test
    fun `non-digit input returns NON_DIGIT error and does not call repository`() =
        runTest {
            val result = useCase("123A5678")
            assertEquals(ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.NON_DIGIT), result)
            assertEquals(0, repository.findItemByBarcodeCalls.size)
        }

    @Test
    fun `unsupported length returns UNSUPPORTED_FORMAT error and does not call repository`() =
        runTest {
            val result = useCase("1234567") // 7 digits
            assertEquals(ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.UNSUPPORTED_FORMAT), result)
            assertEquals(0, repository.findItemByBarcodeCalls.size)
        }

    @Test
    fun `invalid check digit returns INVALID_CHECK_DIGIT error and does not call repository`() =
        runTest {
            val result = useCase("4006381333932") // Valid is ...3931
            assertEquals(ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.INVALID_CHECK_DIGIT), result)
            assertEquals(0, repository.findItemByBarcodeCalls.size)
        }

    @Test
    fun `canonical whitespace normalization calls repository with trimmed value`() =
        runTest {
            val result = useCase(" 4006381333931 ")
            assertTrue(result is ResolveScannedBarcodeResult.NewBarcode)
            assertEquals("4006381333931", (result as ResolveScannedBarcodeResult.NewBarcode).barcode.value)
            assertEquals(1, repository.findItemByBarcodeCalls.size)
            assertEquals("4006381333931", repository.findItemByBarcodeCalls[0].value)
        }

    @Test
    fun `NotFound returns NewBarcode`() =
        runTest {
            val barcodeValue = "4006381333931"
            val result = useCase(barcodeValue)
            assertTrue(result is ResolveScannedBarcodeResult.NewBarcode)
            assertEquals(barcodeValue, (result as ResolveScannedBarcodeResult.NewBarcode).barcode.value)
        }

    @Test
    fun `Found returns ExistingItem`() =
        runTest {
            val barcodeValue = "4006381333931"
            val barcode = (Barcode.create(barcodeValue) as BarcodeCreationResult.Success).barcode
            val householdId = HouseholdId.from("h1")
            val productId = ProductId.from("p1")
            val nameResult = ProductName.create("Stabilo") as ProductNameCreationResult.Success
            val item =
                InventoryItem(
                    product =
                        Product(
                            id = productId,
                            householdId = householdId,
                            name = nameResult.productName,
                            barcode = barcode,
                        ),
                    stockItem =
                        StockItem(
                            id = StockItemId.from("stock-1"),
                            householdId = householdId,
                            productId = productId,
                            quantity = ExactQuantity.of(BigDecimal.ONE, MeasurementUnit.UNIT),
                        ),
                )
            repository.emit(listOf(item))

            val result = useCase(barcodeValue)
            assertTrue(result is ResolveScannedBarcodeResult.ExistingItem)
            assertEquals("stock-1", (result as ResolveScannedBarcodeResult.ExistingItem).stockItemId.value)
        }

    @Test
    fun `StorageFailure returns StorageFailure`() =
        runTest {
            repository.setFindResult(FindItemByBarcodeResult.StorageFailure)
            val result = useCase("4006381333931")
            assertEquals(ResolveScannedBarcodeResult.StorageFailure, result)
        }
}
