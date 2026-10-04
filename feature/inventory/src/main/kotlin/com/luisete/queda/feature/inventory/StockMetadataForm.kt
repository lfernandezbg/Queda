@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda.feature.inventory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.luisete.queda.core.designsystem.component.QuedaChoiceChip
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.domain.inventory.StockWriteResult
import com.luisete.queda.core.model.inventory.FoodType
import com.luisete.queda.core.model.inventory.StorageLocation

@Composable
fun LocationChoices(
    locations: List<StorageLocation>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Text(stringResource(R.string.stock_location), style = MaterialTheme.typography.labelLarge)
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(QuedaSpacing.Small),
    ) {
        QuedaChoiceChip(stringResource(R.string.stock_no_location), selected == null, { onSelect(null) })
        locations.filter { !it.archived || it.id == selected }.forEach { location ->
            QuedaChoiceChip(
                location.name,
                selected == location.id,
                { onSelect(location.id) },
                Modifier.testTag("location_${location.id}"),
            )
        }
    }
}

@Composable
fun StockMetadataForm(
    form: StockForm,
    locations: List<StorageLocation>,
    onChange: (StockForm) -> Unit,
    editing: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small)) {
        if (editing) {
            QuedaTextField(
                form.name,
                { onChange(form.copy(name = it)) },
                stringResource(R.string.stock_name),
            )
            QuedaTextField(
                form.label,
                { onChange(form.copy(label = it)) },
                stringResource(R.string.stock_label),
            )
        }
        LocationChoices(locations, form.locationId) { onChange(form.copy(locationId = it)) }
        Column {
            FoodType.entries.forEach { type ->
                QuedaChoiceChip(
                    stringResource(if (type == FoodType.FOOD) R.string.stock_food else R.string.stock_prepared),
                    form.foodType == type,
                    { onChange(form.copy(foodType = type, preparedOn = "")) },
                )
            }
        }
        if (form.foodType == FoodType.PREPARED) {
            QuedaTextField(
                form.preparedOn,
                { onChange(form.copy(preparedOn = it)) },
                stringResource(R.string.stock_prepared_on),
            )
        }
        QuedaTextField(
            form.bestBefore,
            { onChange(form.copy(bestBefore = it)) },
            stringResource(R.string.stock_best_before),
        )
    }
}

@Composable
fun StockResultText(result: StockWriteResult?) {
    if (result != null) {
        Text(
            stringResource(
                when (result) {
                    StockWriteResult.SAVED -> R.string.stock_saved
                    StockWriteResult.INVALID -> R.string.stock_invalid
                    StockWriteResult.STALE -> R.string.stock_stale
                    StockWriteResult.DUPLICATE -> R.string.stock_duplicate
                    StockWriteResult.STORAGE_FAILURE -> R.string.stock_storage_failure
                },
            ),
            color =
                if (result == StockWriteResult.SAVED) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
    }
}
