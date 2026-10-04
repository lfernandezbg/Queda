@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.theme.QuedaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ManagedInventoryRow(
    item: InventoryItemUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onConsume: () -> Unit,
    selecting: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val dismiss = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismiss.currentValue, selecting) {
        if (selecting) return@LaunchedEffect
        when (dismiss.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                dismiss.snapTo(SwipeToDismissBoxValue.Settled)
                onConsume()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                dismiss.snapTo(SwipeToDismissBoxValue.Settled)
                onEdit()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = !selecting,
        enableDismissFromEndToStart = !selecting,
        backgroundContent = {
            Row(
                Modifier.fillMaxWidth().padding(QuedaSpacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.stock_consume_item, item.name))
                Text(stringResource(R.string.stock_edit))
            }
        },
    ) {
        Column {
            if (selecting) {
                val label = stringResource(R.string.stock_select_item, item.name)
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.semantics { contentDescription = label },
                )
            }
            InventoryItemRow(item, if (selecting) onSelect else onClick, onSelect)
            item.details.bestBefore?.let { Text(stringResource(R.string.stock_due_date, it.toString())) }
            if (!selecting) {
                Row(horizontalArrangement = Arrangement.spacedBy(QuedaSpacing.Small)) {
                    QuedaSecondaryButton(stringResource(R.string.stock_edit), onEdit, Modifier.weight(1f))
                    QuedaSecondaryButton(stringResource(R.string.stock_consume_short), onConsume, Modifier.weight(1f))
                }
            }
        }
    }
}
