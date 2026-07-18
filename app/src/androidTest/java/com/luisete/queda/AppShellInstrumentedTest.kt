package com.luisete.queda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.luisete.queda.feature.inventory.InventoryTestTags
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.onAllNodesWithTag
class AppShellInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_and_navigates_to_add_item_and_back() {
        // Assert Inventory Screen
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_SCREEN).assertIsDisplayed()

        // Tap Add
        //composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON).performClick()
        // Wait until Inventory finishes loading and exposes the Add action.
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule
                .onAllNodesWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

// Tap Add
        composeTestRule
            .onNodeWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON)
            .performClick()

        // Assert Add Exact Item Screen
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_SCREEN).assertIsDisplayed()

        // Tap Back
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_BACK_BUTTON).performClick()

        // Assert Inventory Screen again
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_SCREEN).assertIsDisplayed()
    }
}
