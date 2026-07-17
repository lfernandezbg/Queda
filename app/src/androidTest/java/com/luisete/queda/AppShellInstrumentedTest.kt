package com.luisete.queda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.luisete.queda.core.designsystem.QuedaTestTags
import org.junit.Rule
import org.junit.Test

class AppShellInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_and_shows_inventory_screen() {
        composeTestRule.onNodeWithTag(QuedaTestTags.INVENTORY_SCREEN).assertIsDisplayed()
    }
}
