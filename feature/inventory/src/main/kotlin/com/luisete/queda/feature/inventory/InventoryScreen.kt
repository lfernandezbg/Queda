@file:Suppress("ktlint:standard:function-naming", "TooManyFunctions")

package com.luisete.queda.feature.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaBottomActionBar
import com.luisete.queda.core.designsystem.component.QuedaEmptyState
import com.luisete.queda.core.designsystem.component.QuedaErrorState
import com.luisete.queda.core.designsystem.component.QuedaLoadingState
import com.luisete.queda.core.designsystem.component.QuedaModalBottomSheet
import com.luisete.queda.core.designsystem.component.QuedaNumericField
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity

@Composable
@Suppress("FunctionNaming")
fun InventoryRoute(
    viewModel: InventoryViewModel,
    onAddItem: () -> Unit,
    onScanBarcode: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InventoryScreen(
        uiState = uiState,
        onAddItem = onAddItem,
        onScanBarcode = onScanBarcode,
        onRetry = viewModel::retry,
        onItemClick = viewModel::onItemClick,
        onDismissSheet = viewModel::onDismissSheet,
        onSelectConsume = viewModel::onSelectConsume,
        onSelectCorrect = viewModel::onSelectCorrect,
        onAmountChange = viewModel::onAmountChange,
        onUnitChange = viewModel::onUnitChange,
        onConfirm = viewModel::onConfirm,
        onTogglePresence = viewModel::onTogglePresence,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongParameterList", "TooManyFunctions", "LongMethod")
fun InventoryScreen(
    uiState: InventoryUiState,
    onAddItem: () -> Unit,
    onScanBarcode: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (InventoryItemUiModel) -> Unit,
    onDismissSheet: () -> Unit,
    onSelectConsume: () -> Unit,
    onSelectCorrect: () -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onConfirm: () -> Unit,
    onTogglePresence: (Boolean) -> Unit,
) {
    QuedaScaffold(
        modifier =
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag(InventoryTestTags.INVENTORY_SCREEN),
        topBar = {
            QuedaTopAppBar(
                title = stringResource(R.string.inventory_title),
                modifier = Modifier.testTag(InventoryTestTags.INVENTORY_TOP_BAR),
                actions = {
                    IconButton(
                        onClick = onScanBarcode,
                        modifier =
                            Modifier
                                .semantics { testTagsAsResourceId = true }
                                .testTag(InventoryTestTags.INVENTORY_SCAN_BUTTON),
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.inventory_scan_button_description),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is InventoryUiState.Content) {
                QuedaBottomActionBar {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onScanBarcode,
                            modifier =
                                Modifier
                                    .semantics { testTagsAsResourceId = true }
                                    .testTag(InventoryTestTags.INVENTORY_SCAN_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.inventory_scan_button_description),
                            )
                        }
                        Spacer(modifier = Modifier.size(QuedaSpacing.Small))
                        QuedaPrimaryButton(
                            text = stringResource(R.string.inventory_add_button),
                            onClick = onAddItem,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .testTag(InventoryTestTags.INVENTORY_ADD_BUTTON),
                        )
                    }
                }
            }
        },
    ) { padding ->
        InventoryScreenContent(
            uiState = uiState,
            padding = padding,
            onAddItem = onAddItem,
            onScanBarcode = onScanBarcode,
            onRetry = onRetry,
            onItemClick = onItemClick,
        )

        if (uiState is InventoryUiState.Content) {
            if (uiState.quantityAction !is QuantityActionUiState.Closed) {
                QuantityActionSheet(
                    state = uiState.quantityAction,
                    onDismiss = onDismissSheet,
                    onSelectConsume = onSelectConsume,
                    onSelectCorrect = onSelectCorrect,
                    onAmountChange = onAmountChange,
                    onUnitChange = onUnitChange,
                    onConfirm = onConfirm,
                )
            }

            if (uiState.presenceAction !is PresenceActionUiState.Closed) {
                PresenceActionSheet(
                    state = uiState.presenceAction,
                    onDismiss = onDismissSheet,
                    onTogglePresence = onTogglePresence,
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongParameterList", "LongMethod")
private fun InventoryScreenContent(
    uiState: InventoryUiState,
    padding: PaddingValues,
    onAddItem: () -> Unit,
    onScanBarcode: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (InventoryItemUiModel) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
    ) {
        when (uiState) {
            InventoryUiState.Loading -> {
                QuedaLoadingState(
                    modifier = Modifier.testTag(InventoryTestTags.INVENTORY_LOADING),
                    contentDescription = stringResource(R.string.inventory_loading_description),
                )
            }

            InventoryUiState.Empty -> {
                QuedaEmptyState(
                    title = stringResource(R.string.inventory_empty_title),
                    description = stringResource(R.string.inventory_empty_body),
                    modifier = Modifier.testTag(InventoryTestTags.INVENTORY_EMPTY_STATE),
                    action = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            QuedaPrimaryButton(
                                text = stringResource(R.string.inventory_add_button),
                                onClick = onAddItem,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .testTag(InventoryTestTags.INVENTORY_ADD_BUTTON),
                            )
                            Spacer(modifier = Modifier.height(QuedaSpacing.Small))
                            QuedaPrimaryButton(
                                text = stringResource(R.string.inventory_scan_button_label),
                                onClick = onScanBarcode,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .testTag(InventoryTestTags.INVENTORY_SCAN_BUTTON),
                            )
                        }
                    },
                )
            }

            is InventoryUiState.Content -> {
                InventoryList(
                    items = uiState.items,
                    onItemClick = onItemClick,
                )
            }

            InventoryUiState.Error -> {
                QuedaErrorState(
                    message = stringResource(R.string.inventory_error_message),
                    modifier = Modifier.testTag(InventoryTestTags.INVENTORY_ERROR_STATE),
                    action = {
                        QuedaSecondaryButton(
                            text = stringResource(R.string.inventory_retry_button),
                            onClick = onRetry,
                            modifier = Modifier.testTag(InventoryTestTags.INVENTORY_RETRY_BUTTON),
                        )
                    },
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun InventoryList(
    items: List<InventoryItemUiModel>,
    onItemClick: (InventoryItemUiModel) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(InventoryTestTags.INVENTORY_ITEM_LIST),
        contentPadding = PaddingValues(bottom = QuedaSpacing.Medium),
    ) {
        item {
            InventorySummaryHeader(itemsCount = items.size)
        }

        items(
            items = items,
            key = { it.id },
        ) { item ->
            InventoryItemRow(
                item = item,
                onClick = { onItemClick(item) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = QuedaSpacing.Medium),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun InventorySummaryHeader(itemsCount: Int) {
    Text(
        text = pluralStringResource(R.plurals.inventory_summary, itemsCount, itemsCount),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .padding(horizontal = QuedaSpacing.Medium)
                .padding(top = QuedaSpacing.Small, bottom = QuedaSpacing.Medium),
    )
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
fun InventoryItemRow(
    item: InventoryItemUiModel,
    onClick: () -> Unit,
) {
    val quantityText =
        when (val q = item.quantity) {
            is ExactQuantity -> {
                val unitAbbreviation =
                    when (q.unit) {
                        MeasurementUnit.UNIT -> stringResource(R.string.unit_abbreviation_unit)
                        MeasurementUnit.GRAM -> stringResource(R.string.unit_abbreviation_gram)
                        MeasurementUnit.KILOGRAM -> stringResource(R.string.unit_abbreviation_kilogram)
                        MeasurementUnit.MILLILITER -> stringResource(R.string.unit_abbreviation_milliliter)
                        MeasurementUnit.LITER -> stringResource(R.string.unit_abbreviation_liter)
                    }
                stringResource(
                    R.string.inventory_quantity_format,
                    ExactQuantityUiFormatter.format(q),
                    unitAbbreviation,
                )
            }

            is PresenceQuantity -> {
                if (q.isPresent) {
                    stringResource(R.string.presence_status_present)
                } else {
                    stringResource(R.string.presence_status_absent)
                }
            }

            else -> ""
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.inventory_row_click_label),
                )
                .padding(QuedaSpacing.Medium)
                .testTag("${InventoryTestTags.INVENTORY_ITEM_ROW}_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = QuedaSpacing.Medium)
                    .testTag(InventoryTestTags.INVENTORY_ITEM_NAME),
        )
        Text(
            text = quantityText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color =
                if (item.quantity is PresenceQuantity && !(item.quantity as PresenceQuantity).isPresent) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            maxLines = 1,
            modifier =
                Modifier.testTag(
                    if (item.quantity is PresenceQuantity) {
                        InventoryTestTags.INVENTORY_ITEM_PRESENCE_STATUS
                    } else {
                        InventoryTestTags.INVENTORY_ITEM_QUANTITY
                    },
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun PresenceActionSheet(
    state: PresenceActionUiState,
    onDismiss: () -> Unit,
    onTogglePresence: (Boolean) -> Unit,
) {
    if (state is PresenceActionUiState.Managing) {
        QuedaModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(InventoryTestTags.PRESENCE_ACTION_SHEET),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(QuedaSpacing.Medium),
            ) {
                Text(
                    text = state.item.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val statusText =
                    if (state.isPresent) {
                        stringResource(R.string.presence_status_present)
                    } else {
                        stringResource(R.string.presence_status_absent)
                    }
                Text(
                    text = stringResource(R.string.presence_status_current, statusText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(QuedaSpacing.Large))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.presence_status_switch_label),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Switch(
                        checked = state.isPresent,
                        onCheckedChange = onTogglePresence,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.testTag(InventoryTestTags.PRESENCE_STATUS_SWITCH),
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                    )
                }

                if (state.error) {
                    Spacer(modifier = Modifier.height(QuedaSpacing.Small))
                    Text(
                        text = stringResource(R.string.presence_action_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(InventoryTestTags.PRESENCE_ACTION_ERROR),
                    )
                }
                Spacer(modifier = Modifier.height(QuedaSpacing.ExtraLarge))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongParameterList", "LongMethod", "CyclomaticComplexMethod")
private fun QuantityActionSheet(
    state: QuantityActionUiState,
    onDismiss: () -> Unit,
    onSelectConsume: () -> Unit,
    onSelectCorrect: () -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onConfirm: () -> Unit,
) {
    QuedaModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(InventoryTestTags.QUANTITY_ACTION_SHEET),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(QuedaSpacing.Medium),
        ) {
            val item =
                when (state) {
                    is QuantityActionUiState.ActionSelection -> state.item
                    is QuantityActionUiState.ConsumeEditing -> state.item
                    is QuantityActionUiState.CorrectEditing -> state.item
                    else -> null
                }

            item?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val unitRes =
                    when (it.quantity) {
                        is ExactQuantity ->
                            when (it.quantity.unit) {
                                MeasurementUnit.UNIT -> R.string.unit_abbreviation_unit
                                MeasurementUnit.GRAM -> R.string.unit_abbreviation_gram
                                MeasurementUnit.KILOGRAM -> R.string.unit_abbreviation_kilogram
                                MeasurementUnit.MILLILITER -> R.string.unit_abbreviation_milliliter
                                MeasurementUnit.LITER -> R.string.unit_abbreviation_liter
                            }

                        else -> R.string.unit_abbreviation_unit
                    }
                val amountText =
                    if (it.quantity is ExactQuantity) {
                        ExactQuantityUiFormatter.format(it.quantity as ExactQuantity)
                    } else {
                        ""
                    }
                Text(
                    text =
                        stringResource(
                            R.string.inventory_quantity_label,
                            amountText,
                            stringResource(unitRes),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(QuedaSpacing.Medium))
            }

            when (state) {
                is QuantityActionUiState.ActionSelection -> {
                    QuedaPrimaryButton(
                        text = stringResource(R.string.quantity_action_consume),
                        onClick = onSelectConsume,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(InventoryTestTags.QUANTITY_ACTION_CONSUME),
                    )
                    Spacer(modifier = Modifier.height(QuedaSpacing.Small))
                    QuedaSecondaryButton(
                        text = stringResource(R.string.quantity_action_correct),
                        onClick = onSelectCorrect,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(InventoryTestTags.QUANTITY_ACTION_CORRECT),
                    )
                    Spacer(modifier = Modifier.height(QuedaSpacing.Large))
                }

                is QuantityActionUiState.ConsumeEditing -> {
                    val previewText =
                        state.preview?.let {
                            val unitAbbr =
                                when (it.unit) {
                                    MeasurementUnit.UNIT -> stringResource(R.string.unit_abbreviation_unit)
                                    MeasurementUnit.GRAM -> stringResource(R.string.unit_abbreviation_gram)
                                    MeasurementUnit.KILOGRAM -> stringResource(R.string.unit_abbreviation_kilogram)
                                    MeasurementUnit.MILLILITER -> stringResource(R.string.unit_abbreviation_milliliter)
                                    MeasurementUnit.LITER -> stringResource(R.string.unit_abbreviation_liter)
                                }
                            stringResource(R.string.quantity_consume_preview, "${it.amountFormatted} $unitAbbr")
                        }
                    QuantityMutationForm(
                        title = stringResource(R.string.quantity_action_consume_title),
                        amountInput = state.amountInput,
                        selectedUnit = state.selectedUnit,
                        preview = previewText,
                        error = state.error,
                        isSubmitting = state.isSubmitting,
                        confirmLabel = stringResource(R.string.quantity_action_consume_confirm),
                        onAmountChange = onAmountChange,
                        onUnitChange = onUnitChange,
                        onConfirm = onConfirm,
                        onCancel = onDismiss,
                        compatibleUnits =
                            if (state.item.quantity is ExactQuantity) {
                                getCompatibleUnits(state.item.quantity.unit)
                            } else {
                                emptyList()
                            },
                    )
                }

                is QuantityActionUiState.CorrectEditing -> {
                    val previewText =
                        state.preview?.let {
                            val unitAbbr =
                                when (it.unit) {
                                    MeasurementUnit.UNIT -> stringResource(R.string.unit_abbreviation_unit)
                                    MeasurementUnit.GRAM -> stringResource(R.string.unit_abbreviation_gram)
                                    MeasurementUnit.KILOGRAM -> stringResource(R.string.unit_abbreviation_kilogram)
                                    MeasurementUnit.MILLILITER -> stringResource(R.string.unit_abbreviation_milliliter)
                                    MeasurementUnit.LITER -> stringResource(R.string.unit_abbreviation_liter)
                                }
                            stringResource(R.string.quantity_correct_preview, "${it.amountFormatted} $unitAbbr")
                        }
                    QuantityMutationForm(
                        title = stringResource(R.string.quantity_action_correct_title),
                        amountInput = state.amountInput,
                        selectedUnit = state.selectedUnit,
                        preview = previewText,
                        error = state.error,
                        isSubmitting = state.isSubmitting,
                        confirmLabel = stringResource(R.string.quantity_action_correct_confirm),
                        onAmountChange = onAmountChange,
                        onUnitChange = onUnitChange,
                        onConfirm = onConfirm,
                        onCancel = onDismiss,
                        compatibleUnits =
                            if (state.item.quantity is ExactQuantity) {
                                getCompatibleUnits(state.item.quantity.unit)
                            } else {
                                emptyList()
                            },
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongParameterList", "LongMethod")
private fun QuantityMutationForm(
    title: String,
    amountInput: String,
    selectedUnit: MeasurementUnit,
    preview: String?,
    error: QuantityActionError?,
    isSubmitting: Boolean,
    confirmLabel: String,
    onAmountChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    compatibleUnits: List<MeasurementUnit>,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(QuedaSpacing.Small))

    val errorText =
        when (error) {
            QuantityActionError.INVALID_AMOUNT -> stringResource(R.string.error_quantity_format)
            QuantityActionError.MUST_BE_POSITIVE -> stringResource(R.string.error_quantity_not_positive)
            QuantityActionError.MUST_BE_LOWER_THAN_CURRENT -> stringResource(R.string.error_quantity_must_be_lower)
            QuantityActionError.INCOMPATIBLE_UNIT -> stringResource(R.string.error_incompatible_unit)
            QuantityActionError.UNCHANGED -> stringResource(R.string.error_quantity_unchanged)
            QuantityActionError.PRODUCT_NOT_FOUND -> stringResource(R.string.error_product_not_found)
            QuantityActionError.STORAGE_FAILURE -> stringResource(R.string.error_storage_failure)
            null -> null
        }

    QuedaNumericField(
        value = amountInput,
        onValueChange = onAmountChange,
        label = stringResource(R.string.quantity_amount_label),
        modifier =
            Modifier
                .testTag(InventoryTestTags.QUANTITY_ACTION_INPUT)
                .semantics {
                    if (errorText != null) set(SemanticsProperties.Error, errorText)
                },
        enabled = !isSubmitting,
        isError = errorText != null,
        supportingText = errorText,
        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
    )

    Spacer(modifier = Modifier.height(QuedaSpacing.Small))

    UnitSelector(
        selectedUnit = selectedUnit,
        onUnitChange = onUnitChange,
        enabled = !isSubmitting,
        availableUnits = compatibleUnits,
    )

    Spacer(modifier = Modifier.height(QuedaSpacing.Medium))

    if (preview != null) {
        Text(
            text = preview,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag(InventoryTestTags.QUANTITY_ACTION_PREVIEW),
        )
        Spacer(modifier = Modifier.height(QuedaSpacing.Medium))
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        QuedaSecondaryButton(
            text = stringResource(R.string.quantity_action_cancel),
            onClick = onCancel,
            modifier =
                Modifier
                    .weight(1f)
                    .testTag(InventoryTestTags.QUANTITY_ACTION_CANCEL),
            enabled = !isSubmitting,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(QuedaSpacing.Small))
        QuedaPrimaryButton(
            text = if (isSubmitting) stringResource(R.string.quantity_action_submitting) else confirmLabel,
            onClick = onConfirm,
            modifier =
                Modifier
                    .weight(1f)
                    .testTag(InventoryTestTags.QUANTITY_ACTION_CONFIRM),
            enabled = !isSubmitting,
            loading = isSubmitting,
        )
    }
    Spacer(modifier = Modifier.height(QuedaSpacing.Medium))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming")
private fun UnitSelector(
    selectedUnit: MeasurementUnit,
    onUnitChange: (MeasurementUnit) -> Unit,
    enabled: Boolean,
    availableUnits: List<MeasurementUnit>,
) {
    var showSheet by remember { mutableStateOf(false) }
    val units =
        listOf(
            MeasurementUnit.UNIT to R.string.unit_units,
            MeasurementUnit.GRAM to R.string.unit_grams,
            MeasurementUnit.KILOGRAM to R.string.unit_kilograms,
            MeasurementUnit.MILLILITER to R.string.unit_milliliters,
            MeasurementUnit.LITER to R.string.unit_liters,
        ).filter { it.first in availableUnits }

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
                .testTag(InventoryTestTags.QUANTITY_ACTION_UNIT_SELECTOR)
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
            val tag =
                when (unit) {
                    MeasurementUnit.UNIT -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_UNIT
                    MeasurementUnit.GRAM -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_GRAM
                    MeasurementUnit.KILOGRAM -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM
                    MeasurementUnit.MILLILITER -> InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER
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

private fun getCompatibleUnits(unit: MeasurementUnit): List<MeasurementUnit> {
    return MeasurementUnit.entries.filter { it.dimension == unit.dimension }
}

@Preview(showBackground = true, name = "Empty State", group = "Inventory")
@Composable
@Suppress("FunctionNaming")
fun InventoryEmptyPreview() {
    QuedaTheme {
        InventoryScreen(
            uiState = InventoryUiState.Empty,
            onAddItem = {},
            onScanBarcode = {},
            onRetry = {},
            onItemClick = {},
            onDismissSheet = {},
            onSelectConsume = {},
            onSelectCorrect = {},
            onAmountChange = {},
            onUnitChange = {},
            onConfirm = {},
            onTogglePresence = {},
        )
    }
}

@Preview(showBackground = true, name = "Content State", group = "Inventory")
@Composable
@Suppress("FunctionNaming")
fun InventoryContentPreview() {
    QuedaTheme {
        InventoryScreen(
            uiState =
                InventoryUiState.Content(
                    items =
                        listOf(
                            InventoryItemUiModel("1", "Milk", ExactQuantity.of("1", MeasurementUnit.LITER)),
                            InventoryItemUiModel("2", "Bread", ExactQuantity.of("1", MeasurementUnit.UNIT)),
                        ),
                ),
            onAddItem = {},
            onScanBarcode = {},
            onRetry = {},
            onItemClick = {},
            onDismissSheet = {},
            onSelectConsume = {},
            onSelectCorrect = {},
            onAmountChange = {},
            onUnitChange = {},
            onConfirm = {},
            onTogglePresence = {},
        )
    }
}
