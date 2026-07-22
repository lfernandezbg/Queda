package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.BarcodeValidationError
import com.luisete.queda.core.domain.inventory.ResolveScannedBarcodeResult
import com.luisete.queda.core.domain.inventory.ResolveScannedBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class BarcodeScannerViewModel
    @Inject
    constructor(
        private val resolveScannedBarcodeUseCase: ResolveScannedBarcodeUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(BarcodeScannerUiState())
        val uiState = mutableUiState.asStateFlow()

        private val navigationChannel = Channel<BarcodeScannerNavigationEvent>(Channel.BUFFERED)
        val navigationEvents = navigationChannel.receiveAsFlow()

        private val processingGate = AtomicBoolean(false)

        fun onBarcodeDetected(rawBarcode: String) {
            if (rawBarcode.isBlank()) return
            if (!processingGate.compareAndSet(false, true)) return

            mutableUiState.update { it.copy(isProcessing = true, lastError = null) }

            viewModelScope.launch {
                when (val result = resolveScannedBarcodeUseCase(rawBarcode)) {
                    is ResolveScannedBarcodeResult.NewBarcode -> {
                        navigationChannel.send(
                            BarcodeScannerNavigationEvent.ToAddItem(result.barcode.value),
                        )
                        // Keep processingGate as true to block further scans until destroyed
                    }

                    is ResolveScannedBarcodeResult.ExistingItem -> {
                        navigationChannel.send(
                            BarcodeScannerNavigationEvent.ToInventoryWithItem(result.stockItemId.value),
                        )
                        // Keep processingGate as true
                    }

                    is ResolveScannedBarcodeResult.InvalidBarcode -> {
                        mutableUiState.update {
                            it.copy(
                                isProcessing = false,
                                lastError = result.reason.toUiError(),
                            )
                        }
                        processingGate.set(false)
                    }

                    ResolveScannedBarcodeResult.StorageFailure -> {
                        mutableUiState.update {
                            it.copy(
                                isProcessing = false,
                                lastError = BarcodeScannerError.STORAGE_FAILURE,
                            )
                        }
                        processingGate.set(false)
                    }
                }
            }
        }

        private fun BarcodeValidationError.toUiError(): BarcodeScannerError =
            when (this) {
                BarcodeValidationError.BLANK -> BarcodeScannerError.NON_DIGIT
                BarcodeValidationError.NON_DIGIT -> BarcodeScannerError.NON_DIGIT
                BarcodeValidationError.UNSUPPORTED_FORMAT -> BarcodeScannerError.UNSUPPORTED_FORMAT
                BarcodeValidationError.INVALID_CHECK_DIGIT -> BarcodeScannerError.INVALID_CHECK_DIGIT
            }

        fun onPermissionStatusChanged(state: PermissionState) {
            mutableUiState.update { it.copy(permissionState = state) }
        }
    }

data class BarcodeScannerUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isProcessing: Boolean = false,
    val lastError: BarcodeScannerError? = null,
)

enum class PermissionState {
    NOT_REQUESTED,
    REQUESTING,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

enum class BarcodeScannerError {
    INVALID_CHECK_DIGIT,
    UNSUPPORTED_FORMAT,
    NON_DIGIT,
    STORAGE_FAILURE,
}

sealed interface BarcodeScannerNavigationEvent {
    data class ToAddItem(val barcode: String) : BarcodeScannerNavigationEvent

    data class ToInventoryWithItem(val itemId: String) : BarcodeScannerNavigationEvent
}
