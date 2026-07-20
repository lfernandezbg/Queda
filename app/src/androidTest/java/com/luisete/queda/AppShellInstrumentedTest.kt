package com.luisete.queda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.feature.inventory.InventoryTestTags
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppShellInstrumentedTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: QuedaDatabase

    @Before
    fun resetState() {
        hiltRule.inject()
        // Execute clearAllTables off the main thread
        database.clearAllTables()

        // Recreate activity to ensure UI reflects cleared state
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.recreate()
        }

        // Wait until Inventory exposes its deterministic empty/add state
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule
                .onAllNodesWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun app_launches_and_navigates_to_add_item_and_back() {
        // Wait specifically for INVENTORY_ADD_BUTTON
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
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

    @Test
    fun inventory_mutation_flow() {
        val itemName = "Integracion Deterministica"

        // 1. Wait until Inventory exposes the empty/add state
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // 2. Add the uniquely named item
        composeTestRule.onNodeWithTag(InventoryTestTags.INVENTORY_ADD_BUTTON).performClick()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).performTextInput(itemName)
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).performTextInput("10")
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_SAVE_BUTTON).performClick()

        // 3. Wait for exact starting quantity
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(hasText(itemName)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("10 ud").assertIsDisplayed()

        // 4. Open quantity actions
        composeTestRule.onNodeWithText(itemName).performClick()

        // 5. Consume and wait for exact remainder
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CONSUME).performClick()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_INPUT).performTextInput("3")
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CONFIRM).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(hasText("7 ud")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("7 ud").assertIsDisplayed()

        // 6. Correct and wait for exact final quantity
        composeTestRule.onNodeWithText(itemName).performClick()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CORRECT).performClick()
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_INPUT).performTextInput("12")
        composeTestRule.onNodeWithTag(InventoryTestTags.QUANTITY_ACTION_CONFIRM).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(hasText("12 ud")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("12 ud").assertIsDisplayed()
    }
}
