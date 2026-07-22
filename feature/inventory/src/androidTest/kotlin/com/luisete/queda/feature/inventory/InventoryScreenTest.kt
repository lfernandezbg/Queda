package com.luisete.queda.feature.inventory

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InventoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_showsProgressAndAccessibilityDescription() {
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Loading,
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
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_LOADING).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Cargando inventario…").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsExactTextsAndExactlyOneAddButton() {
        composeTestRule.setContent {
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
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_EMPTY_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu inventario está vacío").assertIsDisplayed()
        composeTestRule.onNodeWithText("Añade tu primer alimento para saber qué tienes en casa.").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON).assertCountEquals(1)
    }

    @Test
    fun contentShowsNameAndFormattedQuantityWithStableTags() {
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("1", MeasurementUnit.LITER))
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
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
        composeTestRule.onNodeWithTag("${InventoryTestTags.INVENTORY_ITEM_ROW}_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_NAME, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_QUANTITY, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 l").assertIsDisplayed()
    }

    @Test
    fun tappingRow_opensActionSheet() {
        var clickedItem: InventoryItemUiModel? = null
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("1", MeasurementUnit.LITER))
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
                onAddItem = {},
                onScanBarcode = {},
                onRetry = {},
                onItemClick = { clickedItem = it },
                onDismissSheet = {},
                onSelectConsume = {},
                onSelectCorrect = {},
                onAmountChange = {},
                onUnitChange = {},
                onConfirm = {},
                onTogglePresence = {},
            )
        }
        composeTestRule.onNodeWithTag("${InventoryTestTags.INVENTORY_ITEM_ROW}_1").performClick()
        assertEquals(item, clickedItem)
    }

    @Test
    fun actionSheet_showsProductAndQuantity() {
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("10", MeasurementUnit.UNIT))
        composeTestRule.setContent {
            InventoryScreen(
                uiState =
                    InventoryUiState.Content(
                        items = listOf(item),
                        quantityAction = QuantityActionUiState.ActionSelection(item),
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
        // Use specification to avoid ambiguity with the list row behind the sheet
        composeTestRule.onNode(
            hasText("Milk") and hasAnyAncestor(hasTestTag(InventoryTestTags.QUANTITY_ACTION_SHEET)),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cantidad actual: 10 ud").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CONSUME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CORRECT).assertIsDisplayed()
    }

    @Test
    fun selectingConsume_opensForm() {
        var consumeSelected = false
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("10", MeasurementUnit.UNIT))
        composeTestRule.setContent {
            InventoryScreen(
                uiState =
                    InventoryUiState.Content(
                        items = listOf(item),
                        quantityAction = QuantityActionUiState.ActionSelection(item),
                    ),
                onAddItem = {},
                onScanBarcode = {},
                onRetry = {},
                onItemClick = {},
                onDismissSheet = {},
                onSelectConsume = { consumeSelected = true },
                onSelectCorrect = {},
                onAmountChange = {},
                onUnitChange = {},
                onConfirm = {},
                onTogglePresence = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CONSUME).performClick()
        assertTrue(consumeSelected)
    }

    @Test
    fun consumeForm_showsPreviewAndUpdates() {
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("10", MeasurementUnit.UNIT))
        composeTestRule.setContent {
            InventoryScreen(
                uiState =
                    InventoryUiState.Content(
                        items = listOf(item),
                        quantityAction =
                            QuantityActionUiState.ConsumeEditing(
                                item = item,
                                amountInput = "3",
                                selectedUnit = MeasurementUnit.UNIT,
                                preview = QuantityPreviewUiModel("7", MeasurementUnit.UNIT),
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
        composeTestRule.onNodeWithText("Consumir alimento").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_INPUT).assert(hasText("3"))
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_PREVIEW).assert(hasText("Quedarán 7 ud"))
    }

    @Test
    fun validation_accessible() {
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("10", MeasurementUnit.UNIT))
        composeTestRule.setContent {
            InventoryScreen(
                uiState =
                    InventoryUiState.Content(
                        items = listOf(item),
                        quantityAction =
                            QuantityActionUiState.ConsumeEditing(
                                item = item,
                                amountInput = "11",
                                selectedUnit = MeasurementUnit.UNIT,
                                error = QuantityActionError.MUST_BE_LOWER_THAN_CURRENT,
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
        composeTestRule.onNodeWithText("La cantidad a consumir debe ser menor que la actual.").assertIsDisplayed()
    }

    @Test
    fun submitting_disablesActions() {
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("10", MeasurementUnit.UNIT))
        composeTestRule.setContent {
            InventoryScreen(
                uiState =
                    InventoryUiState.Content(
                        items = listOf(item),
                        quantityAction =
                            QuantityActionUiState.ConsumeEditing(
                                item = item,
                                selectedUnit = MeasurementUnit.UNIT,
                                isSubmitting = true,
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
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CONFIRM).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CANCEL).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_INPUT).assertIsNotEnabled()
    }

    @Test
    fun itemRow_semantics_exposeAction() {
        val item = InventoryItemUiModel("1", "Milk", ExactQuantity.of("1", MeasurementUnit.LITER))
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
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
        composeTestRule.onNodeWithTag("${InventoryTestTags.INVENTORY_ITEM_ROW}_1").assert(hasClickAction())
    }

    @Test
    fun summaryHeader_showsSingular() {
        val item1 = InventoryItemUiModel("1", "Milk", ExactQuantity.of("1", MeasurementUnit.LITER))

        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item1)),
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
        composeTestRule.onNodeWithText("1 alimento").assertIsDisplayed()
    }

    @Test
    fun allFiveUnitsUseExpectedVisibleFormat() {
        val items =
            listOf(
                InventoryItemUiModel("1", "A", ExactQuantity.of("6", MeasurementUnit.UNIT)),
                InventoryItemUiModel("2", "B", ExactQuantity.of("500", MeasurementUnit.GRAM)),
                InventoryItemUiModel("3", "C", ExactQuantity.of("1.5", MeasurementUnit.KILOGRAM)),
                InventoryItemUiModel("4", "D", ExactQuantity.of("750", MeasurementUnit.MILLILITER)),
                InventoryItemUiModel("5", "E", ExactQuantity.of("1.25", MeasurementUnit.LITER)),
            )
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(items),
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
        composeTestRule.onNodeWithText("6 ud").assertIsDisplayed()
        composeTestRule.onNodeWithText("500 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,5 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("750 ml").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,25 l").assertIsDisplayed()
    }

    @Test
    fun showsScanButtons() {
        composeTestRule.setContent {
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
        // Should find at least one (Top bar or empty state)
        composeTestRule.onAllNodesWithTag(InventoryTestTags.INVENTORY_SCAN_BUTTON).onFirst().assertIsDisplayed()
    }

    @Test
    fun addButtonInvokesCallbackOnce() {
        var count = 0
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Empty,
                onAddItem = { count++ },
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
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON).performClick()
        assertEquals(1, count)
    }

    @Test
    fun presenceRow_showsHayOrNoHay() {
        val items =
            listOf(
                InventoryItemUiModel("1", "Salt", PresenceQuantity(true)),
                InventoryItemUiModel("2", "Sugar", PresenceQuantity(false)),
            )
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(items),
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
        composeTestRule.onNodeWithText("Salt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sugar").assertIsDisplayed()
        composeTestRule.onNodeWithText("No hay").assertIsDisplayed()
    }

    @Test
    fun presenceActionSheet_showsStateAndSwitch() {
        val item = InventoryItemUiModel("1", "Salt", PresenceQuantity(true))
        composeTestRule.setContent {
            InventoryScreen(
                uiState =
                    InventoryUiState.Content(
                        items = listOf(item),
                        presenceAction = PresenceActionUiState.Managing(item, isPresent = true),
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
        composeTestRule.onNodeWithTag(InventoryTestTags.PRESENCE_ACTION_SHEET).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Salt") and hasAnyAncestor(hasTestTag(InventoryTestTags.PRESENCE_ACTION_SHEET)),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Estado actual: Hay").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.PRESENCE_STATUS_SWITCH).assertIsDisplayed()
    }
}
