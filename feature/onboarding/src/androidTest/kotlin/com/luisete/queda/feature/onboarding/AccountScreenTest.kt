package com.luisete.queda.feature.onboarding

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.domain.auth.AccountAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recoveryIsAvailableWithEmailAndNoPassword() {
        var action: AccountAction? = null
        compose.setContent {
            QuedaTheme {
                AccountScreen(
                    AccountState(email = "user@example.com"),
                    {},
                    {},
                    {},
                    onMode = { action = it },
                )
            }
        }
        compose.onNodeWithText("Recuperar contraseña").assertIsEnabled().performClick()
        assertEquals(AccountAction.RECOVER, action)
    }
}
