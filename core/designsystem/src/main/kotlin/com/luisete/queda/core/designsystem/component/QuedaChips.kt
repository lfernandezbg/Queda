@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.luisete.queda.core.designsystem.theme.QuedaSpacing

@Composable
@Suppress("FunctionNaming")
fun QuedaChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors =
            SuggestionChipDefaults.suggestionChipColors(
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

@Composable
@Suppress("FunctionNaming")
fun QuedaStatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = QuedaSpacing.Small, vertical = QuedaSpacing.Tiny),
        )
    }
}
