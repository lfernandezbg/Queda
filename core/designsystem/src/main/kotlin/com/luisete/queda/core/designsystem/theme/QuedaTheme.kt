@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private const val TEAL_PRIMARY_VAL = 0xFF00695C
private const val TEAL_SECONDARY_VAL = 0xFF4DB6AC
private const val AMBER_ATTENTION_VAL = 0xFFFFB300
private const val TEAL_PRIMARY_DARK_VAL = 0xFF80CBC4
private const val TEAL_SECONDARY_DARK_VAL = 0xFF004D40

private val TealPrimary = Color(TEAL_PRIMARY_VAL)
private val TealSecondary = Color(TEAL_SECONDARY_VAL)
private val AmberAttention = Color(AMBER_ATTENTION_VAL)

private val TealPrimaryDark = Color(TEAL_PRIMARY_DARK_VAL)
private val TealSecondaryDark = Color(TEAL_SECONDARY_DARK_VAL)

private val DarkColorScheme =
    darkColorScheme(
        primary = TealPrimaryDark,
        secondary = TealSecondaryDark,
        tertiary = AmberAttention,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = TealPrimary,
        secondary = TealSecondary,
        tertiary = AmberAttention,
    )

@Suppress("FunctionNaming")
@Composable
fun QuedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
