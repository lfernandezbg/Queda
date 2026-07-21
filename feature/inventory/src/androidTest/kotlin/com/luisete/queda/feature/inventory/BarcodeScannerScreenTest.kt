package com.luisete.queda.feature.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BarcodeScannerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeCameraContentTag = "fake_camera_content"

    @Test
    fun showsRequestingProgress() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState = BarcodeScannerUiState(permissionState = PermissionState.REQUESTING),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag("barcode_scanner_requesting_progress").assertIsDisplayed()
    }

    @Test
    fun showsPermissionExplanationWhenDenied() {
        var retryInvoked = false
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState = BarcodeScannerUiState(permissionState = PermissionState.DENIED),
                onBack = {},
                onRetryPermission = { retryInvoked = true },
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_PERMISSION_DENIED).assertIsDisplayed()
        composeTestRule.onNodeWithText("Se necesita permiso de cámara para escanear códigos de barras.").assertIsDisplayed()

        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_PERMISSION_RETRY_BUTTON).performClick()
        assertTrue(retryInvoked)
    }

    @Test
    fun showsOpenSettingsWhenPermanentlyDenied() {
        var openSettingsInvoked = false
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState = BarcodeScannerUiState(permissionState = PermissionState.PERMANENTLY_DENIED),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = { openSettingsInvoked = true },
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_PERMISSION_PERMANENTLY_DENIED).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Has denegado el permiso de cámara permanentemente. Debes activarlo en los ajustes.",
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_OPEN_SETTINGS_BUTTON).performClick()
        assertTrue(openSettingsInvoked)
    }

    @Test
    fun grantedStateRendersScannerContent() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState = BarcodeScannerUiState(permissionState = PermissionState.GRANTED),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {
                    Box(Modifier.fillMaxSize().testTag(fakeCameraContentTag))
                },
            )
        }
        composeTestRule.onNodeWithTag(fakeCameraContentTag).assertIsDisplayed()
    }

    @Test
    fun showsInvalidCheckDigitError() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState =
                    BarcodeScannerUiState(
                        permissionState = PermissionState.GRANTED,
                        lastError = BarcodeScannerError.INVALID_CHECK_DIGIT,
                    ),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Código no válido (dígito de control incorrecto).").assertIsDisplayed()
    }

    @Test
    fun showsNonDigitError() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState =
                    BarcodeScannerUiState(
                        permissionState = PermissionState.GRANTED,
                        lastError = BarcodeScannerError.NON_DIGIT,
                    ),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("El código debe contener solo números.").assertIsDisplayed()
    }

    @Test
    fun showsUnsupportedFormatError() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState =
                    BarcodeScannerUiState(
                        permissionState = PermissionState.GRANTED,
                        lastError = BarcodeScannerError.UNSUPPORTED_FORMAT,
                    ),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Formato de código no compatible.").assertIsDisplayed()
    }

    @Test
    fun showsStorageFailureError() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState =
                    BarcodeScannerUiState(
                        permissionState = PermissionState.GRANTED,
                        lastError = BarcodeScannerError.STORAGE_FAILURE,
                    ),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Error al buscar el producto. Inténtalo de nuevo.").assertIsDisplayed()
    }

    @Test
    fun invokesOnBack() {
        var backInvoked = false
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState = BarcodeScannerUiState(),
                onBack = { backInvoked = true },
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_CLOSE_BUTTON).performClick()
        assertTrue(backInvoked)
    }

    @Test
    fun backButtonHasMinimumTouchTarget() {
        composeTestRule.setContent {
            BarcodeScannerScreen(
                uiState = BarcodeScannerUiState(),
                onBack = {},
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {},
            )
        }
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_CLOSE_BUTTON)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun rendersCorrectlyAtLargeFontScale() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
            ) {
                BarcodeScannerScreen(
                    uiState = BarcodeScannerUiState(permissionState = PermissionState.GRANTED),
                    onBack = {},
                    onRetryPermission = {},
                    onOpenSettings = {},
                    cameraPreviewSlot = {
                        Box(Modifier.fillMaxSize().testTag(fakeCameraContentTag))
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag(fakeCameraContentTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_CLOSE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun rendersCorrectlyInSmallViewport() {
        composeTestRule.setContent {
            Box(Modifier.size(320.dp, 480.dp)) {
                BarcodeScannerScreen(
                    uiState = BarcodeScannerUiState(permissionState = PermissionState.GRANTED),
                    onBack = {},
                    onRetryPermission = {},
                    onOpenSettings = {},
                    cameraPreviewSlot = {
                        Box(Modifier.fillMaxSize().testTag(fakeCameraContentTag))
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag(fakeCameraContentTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(InventoryTestTags.BARCODE_SCANNER_CLOSE_BUTTON).assertIsDisplayed()
    }
}
