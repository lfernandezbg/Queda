@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val DEEP_TEAL_HEX = 0xFF004D40L
private const val DEEP_TEAL_LIGHT_HEX = 0xFF39796BL
private const val NEAR_BLACK_HEX = 0xFF1A1C1EL
private const val RESTRAINED_GREY_HEX = 0xFF44474EL
private const val WARM_BACKGROUND_HEX = 0xFFFAF9F6L
private const val ERROR_RED_HEX = 0xFFBA1A1AL

private val DeepTeal = Color(DEEP_TEAL_HEX)
private val DeepTealLight = Color(DEEP_TEAL_LIGHT_HEX)
private val NearBlack = Color(NEAR_BLACK_HEX)
private val RestrainedGrey = Color(RESTRAINED_GREY_HEX)
private val WarmBackground = Color(WARM_BACKGROUND_HEX)
private val ErrorRed = Color(ERROR_RED_HEX)

private val LightColorScheme =
    lightColorScheme(
        primary = DeepTeal,
        onPrimary = Color.White,
        secondary = DeepTealLight,
        onSecondary = Color.White,
        error = ErrorRed,
        onError = Color.White,
        background = WarmBackground,
        onBackground = NearBlack,
        surface = Color.White,
        onSurface = NearBlack,
        onSurfaceVariant = RestrainedGrey,
    )

private const val DARK_PRIMARY_HEX = 0xFF80CBC4L
private const val DARK_ON_PRIMARY_HEX = 0xFF003730L
private const val DARK_SECONDARY_HEX = 0xFF4DB6ACL
private const val DARK_ERROR_HEX = 0xFFFFB4ABL
private const val DARK_ON_ERROR_HEX = 0xFF690005L
private const val DARK_BACKGROUND_HEX = 0xFF1A1C1EL
private const val DARK_ON_BACKGROUND_HEX = 0xFFE2E2E6L
private const val DARK_ON_SURFACE_VARIANT_HEX = 0xFFC4C6D0L

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(DARK_PRIMARY_HEX),
        onPrimary = Color(DARK_ON_PRIMARY_HEX),
        secondary = Color(DARK_SECONDARY_HEX),
        onSecondary = Color(DARK_ON_PRIMARY_HEX),
        error = Color(DARK_ERROR_HEX),
        onError = Color(DARK_ON_ERROR_HEX),
        background = Color(DARK_BACKGROUND_HEX),
        onBackground = Color(DARK_ON_BACKGROUND_HEX),
        surface = Color(DARK_BACKGROUND_HEX),
        onSurface = Color(DARK_ON_BACKGROUND_HEX),
        onSurfaceVariant = Color(DARK_ON_SURFACE_VARIANT_HEX),
    )

private const val HEADLINE_LARGE_SIZE = 32
private const val HEADLINE_LARGE_LINE_HEIGHT = 40
private const val HEADLINE_MEDIUM_SIZE = 24
private const val HEADLINE_MEDIUM_LINE_HEIGHT = 32
private const val TITLE_LARGE_SIZE = 20
private const val TITLE_LARGE_LINE_HEIGHT = 28
private const val TITLE_MEDIUM_SIZE = 16
private const val TITLE_MEDIUM_LINE_HEIGHT = 24
private const val BODY_LARGE_SIZE = 16
private const val BODY_LARGE_LINE_HEIGHT = 24
private const val BODY_MEDIUM_SIZE = 14
private const val BODY_MEDIUM_LINE_HEIGHT = 20
private const val LABEL_LARGE_SIZE = 14
private const val LABEL_LARGE_LINE_HEIGHT = 20

private val QuedaTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = HEADLINE_LARGE_SIZE.sp,
                lineHeight = HEADLINE_LARGE_LINE_HEIGHT.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = HEADLINE_MEDIUM_SIZE.sp,
                lineHeight = HEADLINE_MEDIUM_LINE_HEIGHT.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = TITLE_LARGE_SIZE.sp,
                lineHeight = TITLE_LARGE_LINE_HEIGHT.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = TITLE_MEDIUM_SIZE.sp,
                lineHeight = TITLE_MEDIUM_LINE_HEIGHT.sp,
                letterSpacing = 0.15.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = BODY_LARGE_SIZE.sp,
                lineHeight = BODY_LARGE_LINE_HEIGHT.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = BODY_MEDIUM_SIZE.sp,
                lineHeight = BODY_MEDIUM_LINE_HEIGHT.sp,
                letterSpacing = 0.25.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = LABEL_LARGE_SIZE.sp,
                lineHeight = LABEL_LARGE_LINE_HEIGHT.sp,
                letterSpacing = 0.1.sp,
            ),
    )

private const val SHAPE_SMALL = 4
private const val SHAPE_MEDIUM = 8
private const val SHAPE_LARGE = 12

private val QuedaShapes =
    Shapes(
        small = RoundedCornerShape(SHAPE_SMALL.dp),
        medium = RoundedCornerShape(SHAPE_MEDIUM.dp),
        large = RoundedCornerShape(SHAPE_LARGE.dp),
    )

@Composable
@Suppress("FunctionNaming")
fun QuedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuedaTypography,
        shapes = QuedaShapes,
        content = content,
    )
}
