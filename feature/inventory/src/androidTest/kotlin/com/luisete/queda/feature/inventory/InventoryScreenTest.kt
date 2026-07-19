package com.luisete.queda.feature.inventory

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.luisete.queda.core.model.quantity.MeasurementUnit
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
                onRetry = {},
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
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_EMPTY_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu inventario está vacío").assertIsDisplayed()
        composeTestRule.onNodeWithText("Añade tu primer alimento para saber qué tienes en casa.").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON).assertCountEquals(1)
    }

    @Test
    fun contentShowsNameAndFormattedQuantityWithStableTags() {
        val item = InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER)
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_ROW).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_QUANTITY).assertIsDisplayed()
        composeTestRule.onNodeWithText("Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 l").assertIsDisplayed()
    }

    @Test
    fun summaryHeader_showsSingular() {
        val item1 = InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER)

        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item1)),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithText("1 alimento").assertIsDisplayed()
    }

    @Test
    fun summaryHeader_showsPlural() {
        val item1 = InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER)
        val item2 = InventoryItemUiModel("2", "Bread", "1", MeasurementUnit.UNIT)

        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item1, item2)),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithText("2 alimentos").assertIsDisplayed()
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
    fun errorState_showsRetryAndInvokesCallback() {
        var retried = false
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Error,
                onAddItem = {},
                onRetry = { retried = true },
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ERROR_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_RETRY_BUTTON).performClick()
        assertTrue("Retry callback should be invoked", retried)
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
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON).performClick()
        assertEquals(1, count)
    }

    @Test
    fun itemRow_hasNoOnClickSemantics() {
        val item = InventoryItemUiModel("1", "Milk", "1", MeasurementUnit.LITER)
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_ROW).assert(hasClickAction().not())
    }

    @Test
    fun longProductName_doesNotRemoveQuantity() {
        val item =
            InventoryItemUiModel(
                "1",
                "Alimento con un nombre extremadamente largo que debería truncarse",
                "999",
                MeasurementUnit.UNIT,
            )
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(listOf(item)),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_QUANTITY).assertIsDisplayed()
        composeTestRule.onNodeWithText("999 ud").assertIsDisplayed()
    }

    @Test
    fun finalRow_canBeScrolledIntoView() {
        val items = List(20) { InventoryItemUiModel(it.toString(), "Item $it", "1", MeasurementUnit.UNIT) }
        composeTestRule.setContent {
            InventoryScreen(
                uiState = InventoryUiState.Content(items),
                onAddItem = {},
                onRetry = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ITEM_LIST)
            .performScrollToNode(hasText("Item 19"))
        composeTestRule.onNodeWithText("Item 19").assertIsDisplayed()
    }
}
