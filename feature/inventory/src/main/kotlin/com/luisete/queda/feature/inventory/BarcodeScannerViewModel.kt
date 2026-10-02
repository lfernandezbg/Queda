@file:Suppress(
    "detekt:LongMethod",
    "detekt:CyclomaticComplexMethod",
    "detekt:ReturnCount",
    "detekt:MaxLineLength",
    "detekt:MagicNumber",
)

package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.BarcodeValidationError
import com.luisete.queda.core.domain.inventory.ExternalProductLookup
import com.luisete.queda.core.domain.inventory.ExternalProductResult
import com.luisete.queda.core.domain.inventory.ResolveScannedBarcodeResult
import com.luisete.queda.core.domain.inventory.ResolveScannedBarcodeUseCase
import com.luisete.queda.core.model.quantity.PresenceQuantity
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
        private val externalProductLookup: ExternalProductLookup,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(BarcodeScannerUiState())
        val uiState = mutableUiState.asStateFlow()

        private val navigationChannel = Channel<BarcodeScannerNavigationEvent>(Channel.BUFFERED)
        val navigationEvents = navigationChannel.receiveAsFlow()

        private val processingGate = AtomicBoolean(false)
        private var lastReviewedBarcode: String? = null
        private var lastReviewFinishedAt = 0L

        fun onBarcodeDetected(rawBarcode: String) {
            if (rawBarcode.isBlank()) return
            // The camera keeps seeing the same packet while the review sheet closes.
            // Allow it again after a short pause so users can scan identical packets deliberately.
            if (mutableUiState.value.continuousMode && rawBarcode == lastReviewedBarcode &&
                System.currentTimeMillis() - lastReviewFinishedAt < 3000L
            ) {
                return
            }
            if (!processingGate.compareAndSet(false, true)) return

            mutableUiState.update { it.copy(isProcessing = true, lastError = null) }

            viewModelScope.launch {
                when (val result = resolveScannedBarcodeUseCase(rawBarcode)) {
                    is ResolveScannedBarcodeResult.NewBarcode -> {
                        val lookup = externalProductLookup.findByBarcode(result.barcode)
                        val name = (lookup as? ExternalProductResult.Found)?.name
                        val feedback =
                            when (lookup) {
                                is ExternalProductResult.Found -> ProductLookupFeedback.FOUND
                                ExternalProductResult.NotFound -> ProductLookupFeedback.NOT_FOUND
                                ExternalProductResult.MissingName -> ProductLookupFeedback.MISSING_NAME
                                ExternalProductResult.Unavailable -> ProductLookupFeedback.UNAVAILABLE
                            }
                        if (mutableUiState.value.continuousMode) {
                            mutableUiState.update { it.copy(pendingScan = PendingScan.New(result.barcode.value, name, feedback)) }
                        } else {
                            navigationChannel.send(BarcodeScannerNavigationEvent.ToAddItem(result.barcode.value, name, feedback))
                        }
                        // Keep processingGate as true to block further scans until destroyed
                    }

                    is ResolveScannedBarcodeResult.ExistingItem -> {
                        if (mutableUiState.value.continuousMode) {
                            mutableUiState.update {
                                it.copy(
                                    pendingScan =
                                        PendingScan.Existing(
                                            result.stockItemId.value,
                                            rawBarcode,
                                            result.name.orEmpty(),
                                            result.quantity is PresenceQuantity,
                                        ),
                                )
                            }
                        } else {
                            navigationChannel.send(BarcodeScannerNavigationEvent.ToInventoryWithItem(result.stockItemId.value))
                        }
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

        fun setContinuousMode(enabled: Boolean) {
            if (mutableUiState.value.pendingScan == null) mutableUiState.update { it.copy(continuousMode = enabled) }
        }

        fun resume() {
            lastReviewedBarcode =
                when (val pending = mutableUiState.value.pendingScan) {
                    is PendingScan.New -> pending.barcode
                    is PendingScan.Existing -> pending.barcode
                    null -> null
                }
            lastReviewFinishedAt = System.currentTimeMillis()
            mutableUiState.update { it.copy(isProcessing = false, pendingScan = null, lastError = null) }
            processingGate.set(false)
        }
    }

data class BarcodeScannerUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isProcessing: Boolean = false,
    val lastError: BarcodeScannerError? = null,
    val continuousMode: Boolean = false,
    val pendingScan: PendingScan? = null,
)

sealed interface PendingScan {
    data class New(val barcode: String, val suggestedName: String?, val feedback: ProductLookupFeedback) : PendingScan

    data class Existing(val stockItemId: String, val barcode: String, val name: String, val isPresence: Boolean) : PendingScan
}

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
    data class ToAddItem(
        val barcode: String,
        val suggestedName: String? = null,
        val lookupFeedback: ProductLookupFeedback = ProductLookupFeedback.NOT_FOUND,
    ) : BarcodeScannerNavigationEvent

    data class ToInventoryWithItem(val itemId: String) : BarcodeScannerNavigationEvent
}

enum class ProductLookupFeedback {
    FOUND,
    NOT_FOUND,
    MISSING_NAME,
    UNAVAILABLE,
}
