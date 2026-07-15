package com.luisete.queda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.luisete.queda.core.designsystem.QuedaTestTags
import org.junit.Rule
import org.junit.Test

class AppShellInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_and_shows_foundation_message() {
        composeTestRule.onNodeWithTag(QuedaTestTags.APP_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.SCREEN_FOUNDATION).assertIsDisplayed()

        val expectedText = composeTestRule.activity.getString(R.string.foundation_message)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }
}
