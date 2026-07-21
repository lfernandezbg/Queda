package com.luisete.queda.feature.inventory

import com.luisete.queda.core.model.quantity.MeasurementUnit

data class AddExactItemUiState(
    val nameInput: String = "",
    val quantityInput: String = "",
    val selectedUnit: MeasurementUnit = MeasurementUnit.UNIT,
    val barcode: String? = null,
    val nameError: NameInputError? = null,
    val quantityError: QuantityInputError? = null,
    val duplicateError: Boolean = false,
    val duplicateBarcodeError: Boolean = false,
    val storageError: Boolean = false,
    val isSaving: Boolean = false,
)
