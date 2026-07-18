@file:Suppress("MaxLineLength")

package com.luisete.queda.feature.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.QuedaTestTags
import com.luisete.queda.core.designsystem.component.QuedaIconButton
import com.luisete.queda.core.designsystem.component.QuedaModalBottomSheet
import com.luisete.queda.core.designsystem.component.QuedaNumericField
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.feature.inventory.R

@Composable
@Suppress("FunctionName")
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
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod", "FunctionName", "ComplexMethod")
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
        focusRequester.requestFocus()
    }

    BackHandler(enabled = uiState.isSaving) {
        // Block back during saving
    }

    QuedaScaffold(
        modifier = Modifier.testTag(QuedaTestTags.ADD_EXACT_ITEM_SCREEN),
        topBar = {
            QuedaTopAppBar(
                title = stringResource(R.string.add_exact_item_title),
                navigationIcon = {
                    QuedaIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onCancel,
                        modifier = Modifier.testTag(QuedaTestTags.ADD_EXACT_ITEM_BACK_BUTTON),
                        enabled = !uiState.isSaving,
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(QuedaSpacing.Medium),
            ) {
                val nameErrorText =
                    when (uiState.nameError) {
                        NameInputError.BLANK -> stringResource(R.string.error_name_blank)
                        NameInputError.TOO_LONG -> stringResource(R.string.error_name_too_long)
                        NameInputError.FORBIDDEN_CHARACTER -> stringResource(R.string.error_name_forbidden)
                        null -> if (uiState.duplicateError) stringResource(R.string.error_duplicate_name) else null
                    }

                QuedaTextField(
                    value = uiState.nameInput,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.add_exact_item_name_label),
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .testTag(QuedaTestTags.ADD_EXACT_ITEM_NAME_INPUT)
                            .semantics {
                                if (nameErrorText != null) {
                                    error(nameErrorText)
                                }
                            },
                    enabled = !uiState.isSaving,
                    isError = nameErrorText != null,
                    supportingText = nameErrorText,
                    supportingTextTestTag = if (uiState.duplicateError) QuedaTestTags.ADD_EXACT_ITEM_DUPLICATE_ERROR else QuedaTestTags.ADD_EXACT_ITEM_NAME_ERROR,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Spacer(modifier = Modifier.height(QuedaSpacing.Medium))

                val quantityErrorText =
                    when (uiState.quantityError) {
                        QuantityInputError.BLANK -> stringResource(R.string.error_quantity_blank)
                        QuantityInputError.INVALID_FORMAT -> stringResource(R.string.error_quantity_format)
                        QuantityInputError.NOT_POSITIVE -> stringResource(R.string.error_quantity_not_positive)
                        QuantityInputError.TOO_MANY_DECIMALS -> stringResource(R.string.error_quantity_too_many_decimals)
                        null -> null
                    }

                QuedaNumericField(
                    value = uiState.quantityInput,
                    onValueChange = onQuantityChange,
                    label = stringResource(R.string.add_exact_item_quantity_label),
                    modifier =
                        Modifier
                            .testTag(QuedaTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT)
                            .semantics {
                                if (quantityErrorText != null) {
                                    error(quantityErrorText)
                                }
                            },
                    enabled = !uiState.isSaving,
                    isError = quantityErrorText != null,
                    supportingText = quantityErrorText,
                    supportingTextTestTag = QuedaTestTags.ADD_EXACT_ITEM_QUANTITY_ERROR,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )

                Spacer(modifier = Modifier.height(QuedaSpacing.Medium))

                UnitSelector(
                    selectedUnit = uiState.selectedUnit,
                    onUnitChange = onUnitChange,
                    enabled = !uiState.isSaving,
                )

                if (uiState.storageError) {
                    Text(
                        text = stringResource(R.string.error_storage_failure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .padding(top = QuedaSpacing.Small)
                                .testTag(QuedaTestTags.ADD_EXACT_ITEM_STORAGE_ERROR),
                    )
                }

                Spacer(modifier = Modifier.height(QuedaSpacing.ExtraLarge))

                QuedaPrimaryButton(
                    text = stringResource(R.string.add_exact_item_save_button),
                    onClick = onSave,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(QuedaTestTags.ADD_EXACT_ITEM_SAVE_BUTTON),
                    enabled = !uiState.isSaving,
                    loading = uiState.isSaving,
                    loadingTestTag = QuedaTestTags.ADD_EXACT_ITEM_SAVING,
                )

                Spacer(modifier = Modifier.height(QuedaSpacing.Small))

                QuedaSecondaryButton(
                    text = stringResource(R.string.add_exact_item_cancel_button),
                    onClick = onCancel,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(QuedaTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON),
                    enabled = !uiState.isSaving,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName", "LongMethod")
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

    val selectedUnitName = stringResource(units.find { it.first == selectedUnit }?.second ?: R.string.unit_units)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showSheet = true }
                .padding(vertical = QuedaSpacing.Small)
                .testTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR),
    ) {
        Text(
            text = stringResource(R.string.add_exact_item_unit_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = selectedUnitName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            fontWeight = FontWeight.Medium,
        )
    }

    if (showSheet) {
        QuedaModalBottomSheet(
            onDismissRequest = { showSheet = false },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = QuedaSpacing.Large),
            ) {
                Text(
                    text = stringResource(R.string.add_exact_item_unit_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(QuedaSpacing.Medium),
                )
                units.forEach { (unit, stringRes) ->
                    val isSelected = unit == selectedUnit
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable {
                                    onUnitChange(unit)
                                    showSheet = false
                                }
                                .padding(horizontal = QuedaSpacing.Medium)
                                .testTag(
                                    when (unit) {
                                        MeasurementUnit.UNIT -> QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_UNIT
                                        MeasurementUnit.GRAM -> QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_GRAM
                                        MeasurementUnit.KILOGRAM -> QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM
                                        MeasurementUnit.MILLILITER -> QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER
                                        MeasurementUnit.LITER -> QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_LITER
                                    },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(stringRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
    }
}
