package com.luisete.queda.feature.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InvitationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun copiedInvitationContainsCodeAndJoinInstructions() {
        compose.setContent { QuedaTheme { SettingsScreen("Casa", "Actualizado", "abc123", {}, {}, {}) } }
        compose.onNodeWithText("Copiar invitación").performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        assertTrue(text.contains("abc123"))
        assertTrue(text.contains("Unirse a un hogar"))
        assertTrue(text.contains("7 días"))
    }

    @Test
    fun aMemberCannotGenerateInvitations() {
        compose.setContent { QuedaTheme { SettingsScreen("Casa", "Actualizado", null, {}, {}, {}, canInvite = false) } }
        compose.onNodeWithText("Invitar a alguien").assertDoesNotExist()
        compose.onNodeWithText("La persona propietaria del hogar puede generar invitaciones.").assertExists()
    }
}
