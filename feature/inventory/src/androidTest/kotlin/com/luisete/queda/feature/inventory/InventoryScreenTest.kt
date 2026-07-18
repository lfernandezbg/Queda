package com.luisete.queda.feature.inventory

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.luisete.queda.core.designsystem.QuedaTestTags
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InventoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingStateShowsProgress() {
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Loading,
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_LOADING).assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsExactTextsAndAddButton() {
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Empty,
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_EMPTY_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu inventario está vacío").assertIsDisplayed()
        composeTestRule.onNodeWithText("Añade tu primer alimento para saber qué tienes en casa.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_ADD_BUTTON).assertIsDisplayed()
    }

    @Test
    fun contentShowsNameAndFormattedQuantity() {
        val item = InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER)
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithText("Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 l").assertIsDisplayed()
    }

    @Test
    fun allFiveUnitsUseExpectedVisibleFormat() {
        val items =
            listOf(
                InventoryItemUiModel("1", "A", "6", MeasurementUnit.UNIT),
                InventoryItemUiModel("2", "B", "500", MeasurementUnit.GRAM),
                InventoryItemUiModel("3", "C", "1,5", MeasurementUnit.KILOGRAM),
                InventoryItemUiModel("4", "D", "750", MeasurementUnit.MILLILITER),
                InventoryItemUiModel("5", "E", "1,25", MeasurementUnit.LITER),
            )
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(items),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithText("6 ud").assertIsDisplayed()
        composeTestRule.onNodeWithText("500 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,5 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("750 ml").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,25 l").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetry() {
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Error,
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_ERROR_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_RETRY_BUTTON).assertIsDisplayed()
    }

    @Test
    fun addButtonInvokesCallbackOnce() {
        var count = 0
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Empty,
                onAddItem = { count++ },
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_ADD_BUTTON).performClick()
        assertEquals(1, count)
    }

    @Test
    fun repeatedRowsUseStableNonDynamicTags() {
        val items =
            listOf(
                InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER),
                InventoryItemUiModel("2", "Bread", "1", MeasurementUnit.UNIT),
            )
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(items),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onAllNodesWithTag(QuedaTestTags.INVENTORY_ITEM_ROW).assertCountEquals(2)
        composeTestRule.onAllNodesWithTag(QuedaTestTags.INVENTORY_ITEM_NAME).assertCountEquals(2)
        composeTestRule.onAllNodesWithTag(QuedaTestTags.INVENTORY_ITEM_QUANTITY).assertCountEquals(2)
    }
}
