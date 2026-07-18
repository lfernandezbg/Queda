package com.luisete.queda.feature.inventory

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.luisete.queda.core.designsystem.QuedaTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddExactItemScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screenShowsAllRequiredFieldsAndButtons() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_NAME_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_SAVE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON).assertIsDisplayed()
    }

    @Test
    fun backButtonHasCorrectContentDescription() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun unitSelectorShowsAllFiveOptionsInRequiredOrder() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR).performClick()

        val unit = composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_UNIT).getUnclippedBoundsInRoot().top
        val gram = composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_GRAM).getUnclippedBoundsInRoot().top
        val kg = composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM).getUnclippedBoundsInRoot().top
        val ml = composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER).getUnclippedBoundsInRoot().top
        val l = composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_UNIT_OPTION_LITER).getUnclippedBoundsInRoot().top

        assertTrue(unit < gram)
        assertTrue(gram < kg)
        assertTrue(kg < ml)
        assertTrue(ml < l)
    }

    @Test
    fun nameErrorIsVisibleAndAccessible() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(nameError = NameInputError.BLANK),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_NAME_ERROR, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_NAME_INPUT).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Introduce un nombre."),
        )
    }

    @Test
    fun quantityErrorIsVisibleAndAccessible() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(quantityError = QuantityInputError.INVALID_FORMAT),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_QUANTITY_ERROR, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Introduce un número válido."),
        )
    }

    @Test
    fun duplicateErrorIsVisible() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(duplicateError = true),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_DUPLICATE_ERROR, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_NAME_INPUT).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Ya existe un alimento con ese nombre."),
        )
    }

    @Test
    fun storageErrorIsVisible() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(storageError = true),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_STORAGE_ERROR).assertIsDisplayed()
    }

    @Test
    fun savingStateDisablesSaveAndShowsProgress() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(isSaving = true),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_SAVING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_SAVE_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_BACK_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun cancelAndBackInvokeCancelWithoutSave() {
        var cancelCount = 0
        var saveCount = 0
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onSave = { saveCount++ },
                onCancel = { cancelCount++ },
            )
        }
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON).performClick()
        composeTestRule.onNodeWithTag(QuedaTestTags.ADD_EXACT_ITEM_BACK_BUTTON).performClick()

        assertEquals(2, cancelCount)
        assertEquals(0, saveCount)
    }
}
