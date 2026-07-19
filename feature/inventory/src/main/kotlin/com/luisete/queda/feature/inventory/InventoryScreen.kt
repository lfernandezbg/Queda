@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaBottomActionBar
import com.luisete.queda.core.designsystem.component.QuedaEmptyState
import com.luisete.queda.core.designsystem.component.QuedaErrorState
import com.luisete.queda.core.designsystem.component.QuedaLoadingState
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.model.quantity.MeasurementUnit

@Composable
@Suppress("FunctionNaming")
fun InventoryRoute(
    viewModel: InventoryViewModel,
    onAddItem: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InventoryScreen(
        uiState = uiState,
        onAddItem = onAddItem,
        onRetry = viewModel::retry,
    )
}

@Composable
@Suppress("FunctionNaming")
fun InventoryScreen(
    uiState: InventoryUiState,
    onAddItem: () -> Unit,
    onRetry: () -> Unit,
) {
    QuedaScaffold(
        modifier = Modifier.testTag(InventoryTestTags.INVENTORY_SCREEN),
        topBar = {
            QuedaTopAppBar(
                title = stringResource(R.string.inventory_title),
            )
        },
        bottomBar = {
            if (uiState is InventoryUiState.Content) {
                QuedaBottomActionBar {
                    QuedaPrimaryButton(
                        text = stringResource(R.string.inventory_add_button),
                        onClick = onAddItem,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(InventoryTestTags.INVENTORY_ADD_BUTTON),
                    )
                }
            }
        },
    ) { padding ->
        InventoryScreenContent(
            uiState = uiState,
            padding = padding,
            onAddItem = onAddItem,
            onRetry = onRetry,
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun InventoryScreenContent(
    uiState: InventoryUiState,
    padding: PaddingValues,
    onAddItem: () -> Unit,
    onRetry: () -> Unit,
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
                        QuedaPrimaryButton(
                            text = stringResource(R.string.inventory_add_button),
                            onClick = onAddItem,
                            modifier = Modifier.testTag(InventoryTestTags.INVENTORY_ADD_BUTTON),
                        )
                    },
                )
            }

            is InventoryUiState.Content -> {
                InventoryList(items = uiState.items)
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
private fun InventoryList(items: List<InventoryItemUiModel>) {
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
            InventoryItemRow(item)
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
@Suppress("FunctionNaming")
fun InventoryItemRow(item: InventoryItemUiModel) {
    val unitAbbreviation =
        when (item.unit) {
            MeasurementUnit.UNIT -> stringResource(R.string.unit_abbreviation_unit)
            MeasurementUnit.GRAM -> stringResource(R.string.unit_abbreviation_gram)
            MeasurementUnit.KILOGRAM -> stringResource(R.string.unit_abbreviation_kilogram)
            MeasurementUnit.MILLILITER -> stringResource(R.string.unit_abbreviation_milliliter)
            MeasurementUnit.LITER -> stringResource(R.string.unit_abbreviation_liter)
        }
    val quantityFormatted =
        stringResource(
            R.string.inventory_quantity_format,
            item.amountFormatted,
            unitAbbreviation,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(QuedaSpacing.Medium)
                .testTag(InventoryTestTags.INVENTORY_ITEM_ROW),
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
            text = quantityFormatted,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.testTag(InventoryTestTags.INVENTORY_ITEM_QUANTITY),
        )
    }
}

@Preview(showBackground = true, name = "Empty State", group = "Inventory")
@Composable
@Suppress("FunctionNaming")
fun InventoryEmptyPreview() {
    QuedaTheme {
        InventoryScreen(
            uiState = InventoryUiState.Empty,
            onAddItem = {},
            onRetry = {},
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
                            InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER),
                            InventoryItemUiModel("2", "Bread", "1", MeasurementUnit.UNIT),
                        ),
                ),
            onAddItem = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Item Row", group = "Inventory")
@Composable
@Suppress("FunctionNaming")
fun InventoryItemRowPreview() {
    QuedaTheme {
        InventoryItemRow(
            item =
                InventoryItemUiModel(
                    id = "1",
                    name = "Manzanas",
                    amountFormatted = "5",
                    unit = MeasurementUnit.UNIT,
                ),
        )
    }
}

@Preview(showBackground = true, name = "Summary Header", group = "Inventory")
@Composable
@Suppress("FunctionNaming")
fun InventorySummaryHeaderPreview() {
    QuedaTheme {
        InventorySummaryHeader(itemsCount = 5)
    }
}
