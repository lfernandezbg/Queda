package com.luisete.queda.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DesignSystemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun primaryButton_hasMinimumTouchTarget() {
        composeTestRule.setContent {
            QuedaTheme {
                QuedaPrimaryButton(text = "Target", onClick = {})
            }
        }
        val bounds = composeTestRule.onNodeWithText("Target").getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        assertTrue("Height $height < 48dp", height >= 48.dp)
    }

    @Test
    fun primaryButton_loadingState_isDisabledAndShowsProgress() {
        var clicked = false
        composeTestRule.setContent {
            QuedaTheme {
                QuedaPrimaryButton(text = "Saving", onClick = { clicked = true }, loading = true)
            }
        }
        composeTestRule.onNodeWithText("Saving").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Saving").performClick()
        assertTrue("Click should not be invoked while loading", !clicked)
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun textField_withError_exposesSemantics() {
        composeTestRule.setContent {
            QuedaTheme {
                QuedaTextField(
                    value = "Wrong",
                    onValueChange = {},
                    label = "Label",
                    isError = true,
                    supportingText = "Error Message",
                )
            }
        }
        composeTestRule.onNodeWithText("Error Message").assertIsDisplayed()
        composeTestRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Error Message")).assertIsDisplayed()
    }

    @Test
    fun statusChip_isNotClickable() {
        composeTestRule.setContent {
            QuedaTheme {
                QuedaStatusChip(label = "Info", color = androidx.compose.ui.graphics.Color.Red)
            }
        }
        composeTestRule.onNodeWithText("Info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Info").assert(hasClickAction().not())
    }

    @Test
    fun errorState_rendersActionAndInvokesCallback() {
        var retried = false
        composeTestRule.setContent {
            QuedaTheme {
                QuedaErrorState(
                    message = "Failure",
                    action = {
                        QuedaSecondaryButton(text = "Retry", onClick = { retried = true })
                    },
                )
            }
        }
        composeTestRule.onNodeWithText("Failure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue("Retry callback not invoked", retried)
    }

    @Test
    fun loadingState_showsContentDescription() {
        composeTestRule.setContent {
            QuedaTheme {
                QuedaLoadingState(contentDescription = "Busy...")
            }
        }
        composeTestRule.onNodeWithContentDescription("Busy...").assertIsDisplayed()
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test
    fun modalBottomSheet_exposesContentWithTags() {
        composeTestRule.setContent {
            QuedaTheme {
                QuedaModalBottomSheet(
                    onDismissRequest = {},
                    modifier = Modifier.testTag("sheet_tag"),
                ) {
                    androidx.compose.material3.Text(
                        text = "Inside Modal",
                        modifier = Modifier.testTag("tag_inside"),
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("sheet_tag", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("tag_inside", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Inside Modal").assertIsDisplayed()
    }

    @Test
    fun bottomActionBar_rendersContentAndIsClickable() {
        var clicked = false
        composeTestRule.setContent {
            QuedaTheme {
                QuedaBottomActionBar {
                    QuedaPrimaryButton(text = "Action", onClick = { clicked = true })
                }
            }
        }
        composeTestRule.onNodeWithText("Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Action").performClick()
        assertTrue("Bottom action should be clickable", clicked)
    }
}
