@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaModalBottomSheet
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.model.inventory.StorageLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagementControls(
    viewModel: StockManagementViewModel,
    consumeRequest: List<InventoryItemUiModel>?,
    onCloseConsume: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    var confirmSelection by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    LaunchedEffect(state.result) {
        if (state.result == com.luisete.queda.core.domain.inventory.StockWriteResult.SAVED) {
            confirmSelection = false
            if (consumeRequest != null) onCloseConsume()
        }
    }
    StockSelectionActions(state, viewModel::selectionMode, { confirmSelection = true }, { moving = true })
    StockEditingSheet(viewModel, state, locations)
    if (moving) {
        QuedaModalBottomSheet(onDismissRequest = { moving = false }) {
            Column(Modifier.padding(QuedaSpacing.Medium)) {
                Text(stringResource(R.string.stock_move))
                LocationChoices(locations.filterNot { it.archived }, null) {
                    viewModel.move(it)
                    moving = false
                }
            }
        }
    }
    if (confirmSelection || consumeRequest != null) {
        ConsumeSelectionDialog(viewModel, state, consumeRequest ?: state.selected) {
            confirmSelection = false
            onCloseConsume()
        }
    }
}

@Composable
private fun ConsumeSelectionDialog(
    viewModel: StockManagementViewModel,
    state: StockManagementState,
    requested: List<InventoryItemUiModel>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stock_consume_selected)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small),
            ) {
                Text(stringResource(R.string.stock_consume_amount_hint))
                requested.forEach { item ->
                    Text(item.name)
                    QuedaTextField(
                        state.consumeAmounts[item.id].orEmpty(),
                        { viewModel.consumeAmount(item.id, it) },
                        stringResource(R.string.stock_consume_quantity),
                        enabled = !state.busy,
                    )
                }
                StockResultText(state.result)
            }
        },
        confirmButton = {
            QuedaPrimaryButton(stringResource(R.string.stock_consume_selected), {
                viewModel.consume(requested)
            }, enabled = !state.busy)
        },
        dismissButton = {
            QuedaSecondaryButton(stringResource(R.string.stock_cancel), onDismiss)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockEditingSheet(
    viewModel: StockManagementViewModel,
    state: StockManagementState,
    locations: List<StorageLocation>,
) {
    if (state.editing != null) {
        QuedaModalBottomSheet(onDismissRequest = viewModel::dismiss) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(QuedaSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
            ) {
                Text(stringResource(R.string.stock_edit))
                StockMetadataForm(state.form, locations, viewModel::form, editing = true)
                StockResultText(state.result)
                QuedaPrimaryButton(stringResource(R.string.stock_save), viewModel::saveEdit, enabled = !state.busy)
                QuedaSecondaryButton(stringResource(R.string.stock_cancel), viewModel::dismiss, enabled = !state.busy)
            }
        }
    }
}

@Composable
private fun StockSelectionActions(
    state: StockManagementState,
    onToggle: () -> Unit,
    onConsume: () -> Unit,
    onMove: () -> Unit,
) {
    Column(Modifier.padding(horizontal = QuedaSpacing.Medium)) {
        QuedaSecondaryButton(
            stringResource(if (state.selecting) R.string.stock_cancel else R.string.stock_select),
            onToggle,
            enabled = !state.busy,
        )
        if (state.selecting) {
            Text(stringResource(R.string.stock_selection_count, state.selected.size))
            QuedaSecondaryButton(
                stringResource(R.string.stock_consume_all),
                onConsume,
                enabled = state.selected.isNotEmpty() && !state.busy,
            )
            QuedaSecondaryButton(
                stringResource(R.string.stock_move),
                onMove,
                enabled = state.selected.isNotEmpty() && !state.busy,
            )
        }
        StockResultText(state.result)
    }
}
