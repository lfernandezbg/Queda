@file:Suppress(
    "ktlint:standard:function-naming",
    "detekt:FunctionNaming",
    "detekt:TooManyFunctions",
)

package com.luisete.queda.feature.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaBottomActionBar
import com.luisete.queda.core.designsystem.component.QuedaIconButton
import com.luisete.queda.core.designsystem.component.QuedaModalBottomSheet
import com.luisete.queda.core.designsystem.component.QuedaNumericField
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.model.quantity.MeasurementUnit
import kotlinx.coroutines.delay

private const val FOCUS_REQUEST_DELAY = 100L

@Composable
@Suppress("FunctionNaming")
fun AddExactItemRoute(
    viewModel: AddExactItemViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.successEvent) {
        viewModel.successEvent.collect {
            onBack()
        }
    }

    AddExactItemScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onQuantityChange = viewModel::onQuantityChange,
        onUnitChange = viewModel::onUnitChange,
        onSave = viewModel::save,
        onCancel = onBack,
    )
}

@Composable
@Suppress("FunctionNaming", "LongParameterList")
fun AddExactItemScreen(
    uiState: AddExactItemUiState,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        delay(FOCUS_REQUEST_DELAY)
        focusRequester.requestFocus()
    }

    BackHandler(enabled = uiState.isSaving) { /* Block */ }

    QuedaScaffold(
        modifier = Modifier.testTag(InventoryTestTags.ADD_EXACT_ITEM_SCREEN),
        topBar = {
            AddExactItemTopBar(isSaving = uiState.isSaving, onCancel = onCancel)
        },
        bottomBar = {
            AddExactItemBottomBar(isSaving = uiState.isSaving, onSave = onSave, onCancel = onCancel)
        },
    ) { padding ->
        AddExactItemContent(
            uiState = uiState,
            padding = padding,
            focusRequester = focusRequester,
            onNameChange = onNameChange,
            onQuantityChange = onQuantityChange,
            onUnitChange = onUnitChange,
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = { focusManager.clearFocus() },
        )
    }
}

@Composable
private fun AddExactItemTopBar(
    isSaving: Boolean,
    onCancel: () -> Unit,
) {
    QuedaTopAppBar(
        title = stringResource(R.string.add_exact_item_title),
        navigationIcon = {
            QuedaIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onCancel,
                modifier = Modifier.testTag(InventoryTestTags.ADD_EXACT_ITEM_BACK_BUTTON),
                enabled = !isSaving,
            )
        },
    )
}

@Composable
@Suppress("LongParameterList")
private fun AddExactItemContent(
    uiState: AddExactItemUiState,
    padding: PaddingValues,
    focusRequester: FocusRequester,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(QuedaSpacing.Medium),
    ) {
        AddExactItemForm(
            uiState = uiState,
            focusRequester = focusRequester,
            onNameChange = onNameChange,
            onQuantityChange = onQuantityChange,
            onUnitChange = onUnitChange,
            onNext = onNext,
            onDone = onDone,
        )

        if (uiState.barcode != null) {
            Spacer(modifier = Modifier.height(QuedaSpacing.Medium))
            Text(
                text = stringResource(R.string.add_exact_item_barcode_associated, uiState.barcode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(InventoryTestTags.ADD_EXACT_ITEM_BARCODE_INDICATOR),
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun AddExactItemBottomBar(
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    QuedaBottomActionBar(imePadding = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val buttonText =
                if (isSaving) {
                    stringResource(R.string.add_exact_item_saving_label)
                } else {
                    stringResource(R.string.add_exact_item_save_button)
                }
            QuedaPrimaryButton(
                text = buttonText,
                onClick = onSave,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(InventoryTestTags.ADD_EXACT_ITEM_SAVE_BUTTON),
                enabled = !isSaving,
                loading = isSaving,
            )
            Spacer(modifier = Modifier.height(QuedaSpacing.Small))
            QuedaSecondaryButton(
                text = stringResource(R.string.add_exact_item_cancel_button),
                onClick = onCancel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(InventoryTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON),
                enabled = !isSaving,
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongParameterList")
private fun AddExactItemForm(
    uiState: AddExactItemUiState,
    focusRequester: FocusRequester,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
) {
    NameField(
        input = uiState.nameInput,
        inputError = uiState.nameError,
        duplicateError = uiState.duplicateError,
        isSaving = uiState.isSaving,
        focusRequester = focusRequester,
        onNameChange = onNameChange,
        onNext = onNext,
    )

    Spacer(modifier = Modifier.height(QuedaSpacing.Medium))

    QuantityField(
        input = uiState.quantityInput,
        inputError = uiState.quantityError,
        isSaving = uiState.isSaving,
        onQuantityChange = onQuantityChange,
        onDone = onDone,
    )

    Spacer(modifier = Modifier.height(QuedaSpacing.Medium))

    UnitSelector(
        selectedUnit = uiState.selectedUnit,
        onUnitChange = onUnitChange,
        enabled = !uiState.isSaving,
    )

    if (uiState.duplicateBarcodeError) {
        Text(
            text = stringResource(R.string.error_duplicate_barcode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .padding(top = QuedaSpacing.Small)
                    .testTag(InventoryTestTags.ADD_EXACT_ITEM_DUPLICATE_BARCODE_ERROR),
        )
    }

    if (uiState.storageError) {
        Text(
            text = stringResource(R.string.error_storage_failure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .padding(top = QuedaSpacing.Small)
                    .testTag(InventoryTestTags.ADD_EXACT_ITEM_STORAGE_ERROR),
        )
    }
}

@Composable
@Suppress("FunctionNaming", "LongParameterList")
private fun NameField(
    input: String,
    inputError: NameInputError?,
    duplicateError: Boolean,
    isSaving: Boolean,
    focusRequester: FocusRequester,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val errorText =
        when (inputError) {
            NameInputError.BLANK -> stringResource(R.string.error_name_blank)
            NameInputError.TOO_LONG -> stringResource(R.string.error_name_too_long)
            NameInputError.FORBIDDEN_CHARACTER -> stringResource(R.string.error_name_forbidden)
            null -> if (duplicateError) stringResource(R.string.error_duplicate_name) else null
        }

    QuedaTextField(
        value = input,
        onValueChange = onNameChange,
        label = stringResource(R.string.add_exact_item_name_label),
        modifier =
            Modifier
                .focusRequester(focusRequester)
                .testTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT)
                .semantics {
                    if (errorText != null) set(SemanticsProperties.Error, errorText)
                },
        enabled = !isSaving,
        isError = errorText != null,
        supportingText = errorText,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
    )
}

@Composable
@Suppress("FunctionNaming")
private fun QuantityField(
    input: String,
    inputError: QuantityInputError?,
    isSaving: Boolean,
    onQuantityChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val errorText =
        when (inputError) {
            QuantityInputError.BLANK -> stringResource(R.string.error_quantity_blank)
            QuantityInputError.INVALID_FORMAT -> stringResource(R.string.error_quantity_format)
            QuantityInputError.NOT_POSITIVE -> stringResource(R.string.error_quantity_not_positive)
            QuantityInputError.TOO_MANY_DECIMALS ->
                stringResource(R.string.error_quantity_too_many_decimals)
            null -> null
        }

    QuedaNumericField(
        value = input,
        onValueChange = onQuantityChange,
        label = stringResource(R.string.add_exact_item_quantity_label),
        modifier =
            Modifier
                .testTag(InventoryTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT)
                .semantics {
                    if (errorText != null) set(SemanticsProperties.Error, errorText)
                },
        enabled = !isSaving,
        isError = errorText != null,
        supportingText = errorText,
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
fun UnitSelector(
    selectedUnit: MeasurementUnit,
    onUnitChange: (MeasurementUnit) -> Unit,
    enabled: Boolean,
) {
    var showSheet by remember { mutableStateOf(false) }
    val units =
        listOf(
            MeasurementUnit.UNIT to R.string.unit_units,
            MeasurementUnit.GRAM to R.string.unit_grams,
            MeasurementUnit.KILOGRAM to R.string.unit_kilograms,
            MeasurementUnit.MILLILITER to R.string.unit_milliliters,
            MeasurementUnit.LITER to R.string.unit_liters,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                ) {
                    showSheet = true
                }
                .padding(vertical = QuedaSpacing.Small)
                .testTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR)
                .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = stringResource(R.string.add_exact_item_unit_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val selectedUnitName =
            stringResource(
                units.find { it.first == selectedUnit }?.second ?: R.string.unit_units,
            )
        Text(
            text = selectedUnitName,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            fontWeight = FontWeight.Medium,
        )
    }

    if (showSheet) {
        QuedaModalBottomSheet(onDismissRequest = { showSheet = false }) {
            UnitSheetContent(
                selectedUnit = selectedUnit,
                units = units,
                onUnitChange = {
                    onUnitChange(it)
                    showSheet = false
                },
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun UnitSheetContent(
    selectedUnit: MeasurementUnit,
    units: List<Pair<MeasurementUnit, Int>>,
    onUnitChange: (MeasurementUnit) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = QuedaSpacing.Large)) {
        Text(
            text = stringResource(R.string.add_exact_item_unit_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(QuedaSpacing.Medium),
        )
        units.forEach { (unit, stringRes) ->
            val isSelected = unit == selectedUnit
            val tag =
                when (unit) {
                    MeasurementUnit.UNIT -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_UNIT
                    MeasurementUnit.GRAM -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_GRAM
                    MeasurementUnit.KILOGRAM ->
                        InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM
                    MeasurementUnit.MILLILITER ->
                        InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER
                    MeasurementUnit.LITER -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_LITER
                }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onUnitChange(unit) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = QuedaSpacing.Medium)
                        .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(stringRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Add Exact Item",
    group = "Inventory",
)
@Composable
@Suppress("FunctionNaming")
fun AddExactItemScreenPreview() {
    QuedaTheme {
        AddExactItemScreen(
            uiState = AddExactItemUiState(),
            onNameChange = {},
            onQuantityChange = {},
            onUnitChange = {},
            onSave = {},
            onCancel = {},
        )
    }
}
