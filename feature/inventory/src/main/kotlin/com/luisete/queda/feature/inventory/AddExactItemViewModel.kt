package com.luisete.queda.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.AddExactInventoryItemResult
import com.luisete.queda.core.domain.inventory.AddExactInventoryItemUseCase
import com.luisete.queda.core.domain.inventory.ExactQuantityInputError
import com.luisete.queda.core.domain.inventory.ProductNameCreationError
import com.luisete.queda.core.model.inventory.StockTrackingMode
import com.luisete.queda.core.model.quantity.MeasurementUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExactItemViewModel
    @Inject
    constructor(
        private val addExactInventoryItemUseCase: AddExactInventoryItemUseCase,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val mutableUiState =
            MutableStateFlow(
                AddExactItemUiState(
                    nameInput = savedStateHandle[KEY_NAME] ?: "",
                    quantityInput = savedStateHandle[KEY_QUANTITY] ?: "",
                    selectedUnit = savedStateHandle[KEY_UNIT] ?: MeasurementUnit.UNIT,
                    trackingMode = savedStateHandle[KEY_TRACKING_MODE] ?: StockTrackingMode.EXACT,
                    barcode = savedStateHandle[KEY_BARCODE],
                ),
            )
        val uiState: StateFlow<AddExactItemUiState> = mutableUiState.asStateFlow()

        private val successChannel = Channel<Unit>(Channel.BUFFERED)
        val successEvent = successChannel.receiveAsFlow()

        fun onNameChange(name: String) {
            savedStateHandle[KEY_NAME] = name
            mutableUiState.update {
                it.copy(
                    nameInput = name,
                    nameError = null,
                    duplicateError = false,
                    duplicateBarcodeError = false,
                    storageError = false,
                )
            }
        }

        fun onQuantityChange(quantity: String) {
            savedStateHandle[KEY_QUANTITY] = quantity
            mutableUiState.update {
                it.copy(
                    quantityInput = quantity,
                    quantityError = null,
                    duplicateBarcodeError = false,
                    storageError = false,
                )
            }
        }

        fun onUnitChange(unit: MeasurementUnit) {
            savedStateHandle[KEY_UNIT] = unit
            mutableUiState.update { it.copy(selectedUnit = unit) }
        }

        fun onTrackingModeChange(mode: StockTrackingMode) {
            savedStateHandle[KEY_TRACKING_MODE] = mode
            mutableUiState.update {
                it.copy(
                    trackingMode = mode,
                    quantityError = if (mode == StockTrackingMode.PRESENCE) null else it.quantityError,
                )
            }
        }

        fun onBarcodeAssociated(barcode: String) {
            savedStateHandle[KEY_BARCODE] = barcode
            mutableUiState.update { it.copy(barcode = barcode, duplicateBarcodeError = false) }
        }

        @Suppress("LongMethod")
        fun save() {
            if (mutableUiState.value.isSaving) return

            mutableUiState.update {
                it.copy(
                    isSaving = true,
                    nameError = null,
                    quantityError = null,
                    duplicateError = false,
                    duplicateBarcodeError = false,
                    storageError = false,
                )
            }

            viewModelScope.launch {
                val result =
                    addExactInventoryItemUseCase(
                        rawName = mutableUiState.value.nameInput,
                        rawQuantity = mutableUiState.value.quantityInput,
                        unit = mutableUiState.value.selectedUnit,
                        rawBarcode = mutableUiState.value.barcode,
                        trackingMode = mutableUiState.value.trackingMode,
                    )

                when (result) {
                    is AddExactInventoryItemResult.Added -> {
                        mutableUiState.update { it.copy(isSaving = false) }
                        successChannel.send(Unit)
                    }

                    is AddExactInventoryItemResult.InvalidInput -> {
                        mutableUiState.update {
                            it.copy(
                                isSaving = false,
                                nameError = result.nameReason?.toUiError(),
                                quantityError = result.quantityReason?.toUiError(),
                            )
                        }
                    }

                    AddExactInventoryItemResult.DuplicateProductName -> {
                        mutableUiState.update {
                            it.copy(
                                isSaving = false,
                                duplicateError = true,
                            )
                        }
                    }

                    AddExactInventoryItemResult.DuplicateBarcode -> {
                        mutableUiState.update {
                            it.copy(
                                isSaving = false,
                                duplicateBarcodeError = true,
                            )
                        }
                    }

                    AddExactInventoryItemResult.StorageFailure -> {
                        mutableUiState.update {
                            it.copy(
                                isSaving = false,
                                storageError = true,
                            )
                        }
                    }
                }
            }
        }

        private fun ProductNameCreationError.toUiError(): NameInputError =
            when (this) {
                ProductNameCreationError.Blank -> NameInputError.BLANK
                ProductNameCreationError.TooLong -> NameInputError.TOO_LONG
                ProductNameCreationError.ContainsForbiddenCharacter -> NameInputError.FORBIDDEN_CHARACTER
            }

        private fun ExactQuantityInputError.toUiError(): QuantityInputError =
            when (this) {
                ExactQuantityInputError.Blank -> QuantityInputError.BLANK
                ExactQuantityInputError.InvalidFormat -> QuantityInputError.INVALID_FORMAT
                ExactQuantityInputError.NotPositive -> QuantityInputError.NOT_POSITIVE
                ExactQuantityInputError.TooManyDecimalPlaces -> QuantityInputError.TOO_MANY_DECIMALS
            }

        companion object {
            private const val KEY_NAME = "name"
            private const val KEY_QUANTITY = "quantity"
            private const val KEY_UNIT = "unit"
            private const val KEY_BARCODE = "barcode"
            private const val KEY_TRACKING_MODE = "trackingMode"
        }
    }
