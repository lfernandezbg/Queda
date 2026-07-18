@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.designsystem.theme.QuedaTheme

@Preview(showBackground = true, name = "Buttons", group = "Components")
@Composable
@Suppress("FunctionNaming")
fun ButtonsPreview() {
    QuedaTheme {
        Column(
            modifier = Modifier.padding(QuedaSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small),
        ) {
            QuedaPrimaryButton(text = "Primary Enabled", onClick = {})
            QuedaPrimaryButton(text = "Primary Disabled", onClick = {}, enabled = false)
            QuedaPrimaryButton(text = "Primary Loading", onClick = {}, loading = true)
            QuedaSecondaryButton(text = "Secondary Enabled", onClick = {})
            QuedaDestructiveButton(text = "Destructive Enabled", onClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Fields", group = "Components")
@Composable
@Suppress("FunctionNaming")
fun FieldsPreview() {
    QuedaTheme {
        Column(
            modifier = Modifier.padding(QuedaSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small),
        ) {
            QuedaTextField(value = "Text", onValueChange = {}, label = "Label")
            QuedaTextField(
                value = "Error Content",
                onValueChange = {},
                label = "Error Label",
                isError = true,
                supportingText = "This is an error message",
            )
            QuedaNumericField(value = "123.45", onValueChange = {}, label = "Quantity")
        }
    }
}

@Preview(showBackground = true, name = "Chips", group = "Components")
@Composable
@Suppress("FunctionNaming")
fun ChipsPreview() {
    QuedaTheme {
        Column(
            modifier = Modifier.padding(QuedaSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small),
        ) {
            QuedaChip(label = "Chip", onClick = {})
            QuedaStatusChip(label = "Status Info", color = Color.Blue)
        }
    }
}

@Preview(showBackground = true, name = "States", group = "States")
@Composable
@Suppress("FunctionNaming")
fun StatesPreview() {
    QuedaTheme {
        Column(
            modifier = Modifier.padding(QuedaSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Large),
        ) {
            QuedaSectionHeader(title = "Section Title", subtitle = "Section Subtitle")
        }
    }
}

@Preview(showBackground = true, name = "Empty State", group = "States")
@Composable
@Suppress("FunctionNaming")
fun EmptyStatePreview() {
    QuedaTheme {
        QuedaEmptyState(
            title = "Empty State Title",
            description = "This is a description of the empty state.",
            action = {
                QuedaPrimaryButton(text = "Action", onClick = {})
            },
        )
    }
}

@Preview(showBackground = true, name = "Loading State", group = "States")
@Composable
@Suppress("FunctionNaming")
fun LoadingStatePreview() {
    QuedaTheme {
        QuedaLoadingState(contentDescription = "Loading content...")
    }
}

@Preview(showBackground = true, name = "Error State", group = "States")
@Composable
@Suppress("FunctionNaming")
fun ErrorStatePreview() {
    QuedaTheme {
        QuedaErrorState(
            message = "An error occurred.",
            action = {
                QuedaSecondaryButton(text = "Retry", onClick = {})
            },
        )
    }
}
