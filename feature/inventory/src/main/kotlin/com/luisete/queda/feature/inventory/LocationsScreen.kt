@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.model.inventory.StorageLocation

@Composable
fun LocationsRoute(
    viewModel: LocationsViewModel,
    onBack: () -> Unit,
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    QuedaScaffold(topBar = { QuedaTopAppBar(title = stringResource(R.string.stock_locations)) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(QuedaSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small)) {
                    Text(stringResource(R.string.stock_archive_help))
                    QuedaTextField(
                        editor.name,
                        viewModel::name,
                        stringResource(R.string.stock_location_name),
                        Modifier.testTag("location_name"),
                        enabled = !editor.busy,
                    )
                    QuedaPrimaryButton(
                        stringResource(R.string.stock_save),
                        viewModel::save,
                        Modifier.testTag("location_save"),
                        enabled = !editor.busy,
                    )
                    if (editor.id != null) {
                        QuedaSecondaryButton(
                            stringResource(R.string.stock_cancel),
                            viewModel::cancel,
                        )
                    }
                    StockResultText(editor.result)
                }
            }
            items(locations, key = { it.id }) { location ->
                LocationCard(location, viewModel, editor.busy)
            }
            item { QuedaSecondaryButton(stringResource(R.string.back), onBack, enabled = !editor.busy) }
        }
    }
}

@Composable
private fun LocationCard(
    location: StorageLocation,
    viewModel: LocationsViewModel,
    busy: Boolean,
) {
    Column {
        Text(location.name, style = MaterialTheme.typography.titleMedium)
        if (location.conflicted) {
            Text(stringResource(R.string.location_conflict))
            QuedaSecondaryButton(
                stringResource(R.string.location_keep_local),
                { viewModel.resolve(location, true) },
                enabled = !busy,
            )
            QuedaSecondaryButton(
                stringResource(R.string.location_keep_remote),
                { viewModel.resolve(location, false) },
                enabled = !busy,
            )
        }
        QuedaSecondaryButton(
            stringResource(R.string.stock_rename),
            { viewModel.edit(location) },
            enabled = !busy,
        )
        QuedaSecondaryButton(
            stringResource(if (location.archived) R.string.stock_restore else R.string.stock_archive),
            { viewModel.archive(location) },
            enabled = !busy,
        )
    }
}
