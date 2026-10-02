package com.luisete.queda.feature.shopping

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.shopping.ShoppingEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShoppingScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun quickAddForwardsTypedName() {
        var captured = ""
        rule.setContent {
            QuedaTheme {
                ShoppingScreen(emptyList(), null, { captured = it }, { _, _ -> }, {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("shopping_name").performTextInput("Pan integral")
        rule.onNodeWithTag("shopping_add").performClick()
        rule.runOnIdle { assertEquals("Pan integral", captured) }
    }

    @Test fun purchasedEntryIsVisibleAndCanBeRestored() {
        val id = ShoppingEntryId.from("entry-1")
        var desired: Boolean? = null
        rule.setContent {
            QuedaTheme {
                ShoppingScreen(
                    listOf(ShoppingEntry(id, HouseholdId.from("home"), "Pan", "pan", true, 1)),
                    null,
                    {},
                    { _, state -> desired = state },
                    {},
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("shopping_entry-1").assertIsDisplayed()
        rule.onNodeWithTag("shopping_entry-1").performClick()
        rule.runOnIdle { assertEquals(false, desired) }
    }
}
