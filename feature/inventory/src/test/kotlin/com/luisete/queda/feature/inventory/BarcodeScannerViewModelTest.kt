package com.luisete.queda.feature.inventory

import com.luisete.queda.core.domain.inventory.FindItemByBarcodeResult
import com.luisete.queda.core.domain.inventory.ResolveScannedBarcodeUseCase
import com.luisete.queda.core.testing.FakeInventoryRepository
import com.luisete.queda.core.testing.InventoryTestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeScannerViewModelTest {
    private val repository = FakeInventoryRepository()
    private val resolveScannedBarcodeUseCase = ResolveScannedBarcodeUseCase(repository)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: BarcodeScannerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BarcodeScannerViewModel(resolveScannedBarcodeUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `detected new barcode triggers exactly one navigation`() =
        runTest {
            val barcode = "4006381333931"
            viewModel.onBarcodeDetected(barcode)
            advanceUntilIdle()

            val event = viewModel.navigationEvents.first()
            assertTrue(event is BarcodeScannerNavigationEvent.ToAddItem)
            assertEquals(barcode, (event as BarcodeScannerNavigationEvent.ToAddItem).barcode)
            assertTrue(viewModel.uiState.value.isProcessing)

            // Repeated detections while processing are ignored
            viewModel.onBarcodeDetected(barcode)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isProcessing)
        }

    @Test
    fun `two rapid different valid detections produce only one navigation event`() =
        runTest {
            val barcode1 = "4006381333931"
            val barcode2 = "73513537"

            val events = mutableListOf<BarcodeScannerNavigationEvent>()
            val job =
                launch {
                    viewModel.navigationEvents.toList(events)
                }

            viewModel.onBarcodeDetected(barcode1)
            viewModel.onBarcodeDetected(barcode2)
            advanceUntilIdle()

            assertEquals(1, events.size)
            assertTrue(events[0] is BarcodeScannerNavigationEvent.ToAddItem)
            assertEquals(barcode1, (events[0] as BarcodeScannerNavigationEvent.ToAddItem).barcode)

            job.cancel()
        }

    @Test
    fun `blank detection is ignored`() =
        runTest {
            viewModel.onBarcodeDetected("")
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isProcessing)
            // Check that no events are emitted? (navigationEvents.first() would hang)
        }

    @Test
    fun `detected existing barcode triggers navigation to item`() =
        runTest {
            val barcode = "4006381333931"
            val item = InventoryTestData.createInventoryItem(barcode = barcode)
            repository.emit(listOf(item))

            viewModel.onBarcodeDetected(barcode)
            advanceUntilIdle()

            val event = viewModel.navigationEvents.first()
            assertTrue(event is BarcodeScannerNavigationEvent.ToInventoryWithItem)
            assertEquals(item.stockItem.id.value, (event as BarcodeScannerNavigationEvent.ToInventoryWithItem).itemId)
        }

    @Test
    fun `invalid check digit shows error and allows recovery`() =
        runTest {
            val invalidBarcode = "4006381333932" // Valid EAN-13 length but wrong CD
            viewModel.onBarcodeDetected(invalidBarcode)
            advanceUntilIdle()

            assertEquals(BarcodeScannerError.INVALID_CHECK_DIGIT, viewModel.uiState.value.lastError)
            assertFalse(viewModel.uiState.value.isProcessing)

            // Subsequent valid barcode works
            val validBarcode = "4006381333931"
            viewModel.onBarcodeDetected(validBarcode)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isProcessing)
        }

    @Test
    fun `storage failure exposes error and allows retry`() =
        runTest {
            repository.setFindResult(FindItemByBarcodeResult.StorageFailure)
            viewModel.onBarcodeDetected("4006381333931")
            advanceUntilIdle()

            assertEquals(BarcodeScannerError.STORAGE_FAILURE, viewModel.uiState.value.lastError)
            assertFalse(viewModel.uiState.value.isProcessing)

            repository.setFindResult(null) // Back to normal
            viewModel.onBarcodeDetected("4006381333931")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isProcessing)
        }

    @Test
    fun `permission status changes update state`() {
        viewModel.onPermissionStatusChanged(PermissionState.GRANTED)
        assertEquals(PermissionState.GRANTED, viewModel.uiState.value.permissionState)

        viewModel.onPermissionStatusChanged(PermissionState.PERMANENTLY_DENIED)
        assertEquals(PermissionState.PERMANENTLY_DENIED, viewModel.uiState.value.permissionState)
    }

    @Test
    fun `initial state is correct`() {
        assertEquals(PermissionState.NOT_REQUESTED, viewModel.uiState.value.permissionState)
        assertFalse(viewModel.uiState.value.isProcessing)
    }
}
