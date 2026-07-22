package com.luisete.queda.feature.inventory

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import com.luisete.queda.core.model.inventory.StockTrackingMode
import com.luisete.queda.core.model.quantity.MeasurementUnit
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
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_ITEM_MODE_EXACT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_ITEM_MODE_PRESENCE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_SAVE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON).assertIsDisplayed()
    }

    @Test
    fun nameFieldHasInitialFocus() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).assertIsFocused()
    }

    @Test
    fun nameImeActionNextMovesFocusToQuantity() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).performImeAction()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).assertIsFocused()
    }

    @Test
    fun unitSelectorShowsAllFiveOptionsInRequiredOrder() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR).performClick()

        val unit = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_UNIT).getUnclippedBoundsInRoot().top
        val gram = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_GRAM).getUnclippedBoundsInRoot().top
        val kg = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM).getUnclippedBoundsInRoot().top
        val ml = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER).getUnclippedBoundsInRoot().top
        val l = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_LITER).getUnclippedBoundsInRoot().top

        assertTrue(unit < gram)
        assertTrue(gram < kg)
        assertTrue(kg < ml)
        assertTrue(ml < l)
    }

    @Test
    fun selectingUnitOption_invokesCallbackAndHasSelectedSemantics() {
        var selectedUnit: MeasurementUnit? = null
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(selectedUnit = MeasurementUnit.GRAM),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = { selectedUnit = it },
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR).performClick()

        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_GRAM).assert(isSelected())
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_LITER).assert(isNotSelected())

        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_OPTION_LITER).performClick()
        assertEquals(MeasurementUnit.LITER, selectedUnit)
    }

    @Test
    fun closedSelector_hasButtonRoleAndAnnouncesUnit() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(selectedUnit = MeasurementUnit.KILOGRAM),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onNodeWithText("Kilogramos").assertIsDisplayed()
    }

    @Test
    fun savingState_disablesActions_andShowsProgress() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(isSaving = true),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_SAVE_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_BACK_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Guardando…").assertIsDisplayed()
        composeTestRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
        ).assertIsDisplayed()
    }

    @Test
    fun backButtonHasContentDescription() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun nameErrorIsVisibleAndAccessible() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(nameError = NameInputError.BLANK),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithText("Introduce un nombre.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).assert(
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
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithText("Introduce un número válido.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Introduce un número válido."),
        )
    }

    @Test
    fun duplicateErrorIsVisibleAndAccessible() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(duplicateError = true),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithText("Ya existe un alimento con ese nombre.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).assert(
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
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_STORAGE_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("No se ha podido guardar. Inténtalo de nuevo.").assertIsDisplayed()
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
                onTrackingModeChange = {},
                onSave = { saveCount++ },
                onCancel = { cancelCount++ },
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_CANCEL_BUTTON).performClick()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_BACK_BUTTON).performClick()

        assertEquals(2, cancelCount)
        assertEquals(0, saveCount)
    }

    @Test
    fun selectingPresenceMode_hidesQuantityAndUnit() {
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(trackingMode = StockTrackingMode.PRESENCE),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_QUANTITY_INPUT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_UNIT_SELECTOR).assertDoesNotExist()
    }

    @Test
    fun switchingBetweenModes_invokesCallback() {
        var selectedMode: StockTrackingMode? = null
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(trackingMode = StockTrackingMode.EXACT),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = { selectedMode = it },
                onSave = {},
                onCancel = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_ITEM_MODE_PRESENCE).performClick()
        assertEquals(StockTrackingMode.PRESENCE, selectedMode)

        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_ITEM_MODE_EXACT).performClick()
        assertEquals(StockTrackingMode.EXACT, selectedMode)
    }

    @Test
    fun barcodeIndicatorIsVisibleAndCorrectlyPlaced() {
        val barcode = "4006381333931"
        composeTestRule.setContent {
            AddExactItemScreen(
                uiState = AddExactItemUiState(barcode = barcode),
                onNameChange = {},
                onQuantityChange = {},
                onUnitChange = {},
                onTrackingModeChange = {},
                onSave = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_BARCODE_INDICATOR)
            .assertIsDisplayed()
            .assert(hasText("Código de barras asociado: $barcode"))

        val nameField = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_NAME_INPUT).getUnclippedBoundsInRoot().bottom
        val indicator = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_EXACT_ITEM_BARCODE_INDICATOR).getUnclippedBoundsInRoot().top
        val modeSelector = composeTestRule.onNodeWithTag(InventoryTestTags.ADD_ITEM_MODE_EXACT).getUnclippedBoundsInRoot().top

        assertTrue("Indicator should be below NameField", indicator > nameField)
        assertTrue("ModeSelector should be below Indicator", modeSelector > indicator)
    }
}
