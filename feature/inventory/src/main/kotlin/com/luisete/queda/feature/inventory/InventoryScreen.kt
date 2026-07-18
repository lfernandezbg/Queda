package com.luisete.queda.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.feature.inventory.R
import com.luisete.queda.core.designsystem.QuedaTestTags
import com.luisete.queda.core.designsystem.component.QuedaEmptyState
import com.luisete.queda.core.designsystem.component.QuedaErrorState
import com.luisete.queda.core.designsystem.component.QuedaLoadingState
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.model.quantity.MeasurementUnit

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
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
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "FunctionName")
fun InventoryScreen(
    uiState: InventoryUiState,
    onAddItem: () -> Unit,
    onRetry: () -> Unit,
) {
    QuedaScaffold(
        modifier = Modifier.testTag(QuedaTestTags.INVENTORY_SCREEN),
        topBar = {
            QuedaTopAppBar(
                title = stringResource(R.string.inventory_title),
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when (uiState) {
                InventoryUiState.Loading -> {
                    QuedaLoadingState(
                        modifier = Modifier.testTag(QuedaTestTags.INVENTORY_LOADING),
                    )
                }

                InventoryUiState.Empty -> {
                    QuedaEmptyState(
                        title = stringResource(R.string.inventory_empty_title),
                        description = stringResource(R.string.inventory_empty_body),
                        modifier = Modifier.testTag(QuedaTestTags.INVENTORY_EMPTY_STATE),
                        action = {
                            QuedaPrimaryButton(
                                text = stringResource(R.string.inventory_add_button),
                                onClick = onAddItem,
                                modifier = Modifier.testTag(QuedaTestTags.INVENTORY_ADD_BUTTON),
                            )
                        },
                    )
                }

                is InventoryUiState.Content -> {
                    InventoryContent(
                        items = uiState.items,
                        onAddItem = onAddItem,
                    )
                }

                InventoryUiState.Error -> {
                    QuedaErrorState(
                        message = stringResource(R.string.inventory_error_message),
                        onRetry = onRetry,
                        modifier = Modifier.testTag(QuedaTestTags.INVENTORY_ERROR_STATE),
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun InventoryContent(
    items: List<InventoryItemUiModel>,
    onAddItem: () -> Unit,
) {
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(QuedaTestTags.INVENTORY_ITEM_LIST),
            contentPadding =
                PaddingValues(
                    bottom = 80.dp + navigationPadding,
                ),
        ) {
            item {
                InventorySummaryHeader(itemsCount = items.size)
            }

            items(
                items = items,
                key = { it.id },
            ) { item ->
                InventoryItemRow(item)
                Divider(
                    modifier = Modifier.padding(horizontal = QuedaSpacing.Medium),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                    .padding(QuedaSpacing.Medium)
                    .padding(bottom = navigationPadding),
        ) {
            QuedaPrimaryButton(
                text = stringResource(R.string.inventory_add_button),
                onClick = onAddItem,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(QuedaTestTags.INVENTORY_ADD_BUTTON),
            )
        }
    }
}

@Composable
@Suppress("FunctionName")
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
@Suppress("FunctionName")
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
                .testTag(QuedaTestTags.INVENTORY_ITEM_ROW),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(QuedaTestTags.INVENTORY_ITEM_NAME),
        )
        Text(
            text = quantityFormatted,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag(QuedaTestTags.INVENTORY_ITEM_QUANTITY),
        )
    }
}

@Preview(showBackground = true, name = "Item Row", group = "Inventory")
@Composable
@Suppress("FunctionName")
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
@Suppress("FunctionName")
fun InventorySummaryHeaderPreview() {
    QuedaTheme {
        InventorySummaryHeader(itemsCount = 5)
    }
}
