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
import com.luisete.queda.core.model.quantity.ExactQuantity
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

        fun onBarcodeDetected(rawBarcode: String) {
            if (rawBarcode.isBlank()) {
                if (!processingGate.get()) lastReviewedBarcode = null
                return
            }
            if (mutableUiState.value.continuousMode && rawBarcode == lastReviewedBarcode) return
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
                            mutableUiState.update {
                                it.copy(
                                    pendingScan =
                                        PendingScan.New(
                                            result.barcode.value,
                                            name,
                                            feedback,
                                        ),
                                )
                            }
                        } else {
                            navigationChannel.send(
                                BarcodeScannerNavigationEvent.ToAddItem(
                                    result.barcode.value,
                                    name,
                                    feedback,
                                ),
                            )
                        }
                        // Keep processingGate as true to block further scans until destroyed
                    }

                    is ResolveScannedBarcodeResult.ExistingItem -> {
                        if (result.candidates.size > 1) {
                            val choices =
                                result.candidates.map { candidate ->
                                    PendingScan.Existing(
                                        candidate.stockItem.id.value,
                                        rawBarcode,
                                        candidate.product.name.displayValue,
                                        candidate.stockItem.quantity is PresenceQuantity,
                                        listOfNotNull(
                                            candidate.stockItem.details.label,
                                            candidate.stockItem.details.preparedOn?.toString(),
                                            (candidate.stockItem.quantity as? ExactQuantity)?.let {
                                                "${it.amount.toPlainString()} ${it.unit.name}"
                                            },
                                        ).joinToString(" · ").ifBlank { candidate.stockItem.id.value.takeLast(6) },
                                    )
                                }
                            mutableUiState.update { it.copy(isProcessing = false, lotChoices = choices) }
                        } else {
                            showExisting(
                                PendingScan.Existing(
                                    result.stockItemId.value,
                                    rawBarcode,
                                    result.name.orEmpty(),
                                    result.quantity is PresenceQuantity,
                                ),
                            )
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
            if (mutableUiState.value.pendingScan == null && mutableUiState.value.lotChoices.isEmpty()) {
                mutableUiState.update { it.copy(continuousMode = enabled) }
            }
        }

        fun selectLot(id: String) {
            val selected = mutableUiState.value.lotChoices.firstOrNull { it.stockItemId == id } ?: return
            mutableUiState.update { it.copy(lotChoices = emptyList()) }
            viewModelScope.launch { showExisting(selected) }
        }

        private suspend fun showExisting(item: PendingScan.Existing) {
            if (mutableUiState.value.continuousMode) {
                mutableUiState.update { it.copy(pendingScan = item) }
            } else {
                navigationChannel.send(BarcodeScannerNavigationEvent.ToInventoryWithItem(item.stockItemId))
            }
        }

        fun resume() {
            lastReviewedBarcode =
                when (val pending = mutableUiState.value.pendingScan) {
                    is PendingScan.New -> pending.barcode
                    is PendingScan.Existing -> pending.barcode
                    null -> null
                }
            mutableUiState.update { it.copy(isProcessing = false, pendingScan = null, lotChoices = emptyList(), lastError = null) }
            processingGate.set(false)
        }
    }

data class BarcodeScannerUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isProcessing: Boolean = false,
    val lastError: BarcodeScannerError? = null,
    val continuousMode: Boolean = false,
    val pendingScan: PendingScan? = null,
    val lotChoices: List<PendingScan.Existing> = emptyList(),
)

sealed interface PendingScan {
    data class New(val barcode: String, val suggestedName: String?, val feedback: ProductLookupFeedback) : PendingScan

    data class Existing(
        val stockItemId: String,
        val barcode: String,
        val name: String,
        val isPresence: Boolean,
        val description: String = "",
    ) : PendingScan
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
