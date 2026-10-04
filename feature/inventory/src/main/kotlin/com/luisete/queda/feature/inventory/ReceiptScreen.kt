@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda.feature.inventory

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.luisete.queda.core.designsystem.component.QuedaChoiceChip
import com.luisete.queda.core.designsystem.component.QuedaNumericField
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.domain.inventory.ReceiptResult
import com.luisete.queda.core.model.inventory.ReceiptLine
import com.luisete.queda.core.model.inventory.StorageLocation
import com.luisete.queda.core.model.quantity.MeasurementUnit

private const val MAX_RECEIPT_PAGES = 12

@Composable
fun ReceiptRoute(
    viewModel: ReceiptViewModel,
    management: StockManagementViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locations by management.locations.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var captureStarting by rememberSaveable { mutableStateOf(false) }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) viewModel.recognize(uri.toString())
        }
    val scanner =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            captureStarting = false
            if (result.resultCode == Activity.RESULT_OK) {
                val scanned = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val pages = scanned?.pages.orEmpty().map { it.imageUri.toString() }
                if (pages.isNotEmpty()) viewModel.recognize(pages) else viewModel.unavailable()
            }
        }
    val capture: () -> Unit = capture@{
        if (captureStarting || state.busy) return@capture
        captureStarting = true
        val activity = context.activity()
        if (activity == null) {
            captureStarting = false
            viewModel.unavailable()
        } else {
            val options =
                GmsDocumentScannerOptions.Builder().setPageLimit(MAX_RECEIPT_PAGES).setGalleryImportAllowed(true)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
            GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                .addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
                .addOnFailureListener {
                    captureStarting = false
                    viewModel.unavailable()
                }
        }
    }
    ReceiptScreen(
        state.copy(busy = state.busy || captureStarting),
        locations,
        capture,
        { gallery.launch("image/*") },
        viewModel::edit,
        viewModel::import,
        viewModel::discard,
        onBack,
    )
}

@Composable
@Suppress("LongParameterList")
fun ReceiptScreen(
    state: ReceiptState,
    locations: List<StorageLocation>,
    onCapture: () -> Unit,
    onGallery: () -> Unit,
    onEdit: (String, (ReceiptLine) -> ReceiptLine) -> Unit,
    onImport: () -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
) {
    QuedaScaffold(topBar = { QuedaTopAppBar(stringResource(R.string.receipt_title)) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(QuedaSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
        ) {
            item {
                Text(stringResource(R.string.receipt_help))
                if (state.busy) Text(stringResource(R.string.receipt_busy))
                if (state.draft == null) {
                    QuedaPrimaryButton(stringResource(R.string.receipt_capture), onCapture, enabled = !state.busy)
                    QuedaSecondaryButton(stringResource(R.string.receipt_gallery), onGallery, enabled = !state.busy)
                }
                ReceiptResultText(state.result)
            }
            items(state.draft?.lines.orEmpty(), key = { it.id }) { line ->
                ReceiptLineEditor(line, locations, onEdit, !state.busy)
            }
            item {
                if (state.draft != null) {
                    QuedaPrimaryButton(stringResource(R.string.receipt_import), onImport, enabled = !state.busy)
                    QuedaSecondaryButton(stringResource(R.string.receipt_discard), onDiscard, enabled = !state.busy)
                }
                QuedaSecondaryButton(stringResource(R.string.back), onBack, enabled = !state.busy)
            }
        }
    }
}

@Composable
private fun ReceiptLineEditor(
    line: ReceiptLine,
    locations: List<StorageLocation>,
    onEdit: (String, (ReceiptLine) -> ReceiptLine) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small)) {
        Text(line.source, style = MaterialTheme.typography.bodySmall)
        val includeLabel = stringResource(R.string.receipt_include)
        Row {
            Checkbox(
                line.selected,
                { onEdit(line.id) { current -> current.copy(selected = it) } },
                modifier = Modifier.semantics { contentDescription = includeLabel },
                enabled = enabled,
            )
            Text(stringResource(R.string.receipt_include))
        }
        QuedaTextField(
            line.name,
            { onEdit(line.id) { current -> current.copy(name = it, reviewed = false) } },
            stringResource(R.string.receipt_name),
            Modifier.testTag("receipt_name_${line.id}"),
            enabled = enabled,
        )
        QuedaNumericField(line.quantity, {
            onEdit(line.id) { current -> current.copy(quantity = it, reviewed = false) }
        }, stringResource(R.string.receipt_quantity), enabled = enabled)
        MeasurementUnit.entries.forEach { unit ->
            QuedaChoiceChip(
                stringResource(unit.label()),
                unit == line.unit,
                { if (enabled) onEdit(line.id) { current -> current.copy(unit = unit, reviewed = false) } },
            )
        }
        LocationChoices(locations.filterNot { it.archived }, line.locationId) {
            if (enabled) onEdit(line.id) { current -> current.copy(locationId = it, reviewed = false) }
        }
        val reviewLabel = stringResource(R.string.receipt_reviewed)
        Row {
            Checkbox(
                line.reviewed,
                { onEdit(line.id) { current -> current.copy(reviewed = it) } },
                modifier = Modifier.semantics { contentDescription = reviewLabel },
                enabled = enabled,
            )
            Text(stringResource(R.string.receipt_reviewed))
        }
    }
}

@Composable
private fun ReceiptResultText(result: ReceiptResult?) {
    val resource =
        when (result) {
            ReceiptResult.IMPORTED -> R.string.receipt_imported
            ReceiptResult.DUPLICATE -> R.string.receipt_duplicate
            ReceiptResult.INVALID -> R.string.receipt_invalid
            ReceiptResult.UNREADABLE -> R.string.receipt_unreadable
            ReceiptResult.STORAGE_FAILURE -> R.string.receipt_error
            ReceiptResult.SAVED, null -> null
        }
    if (resource != null) Text(stringResource(resource))
}

private fun MeasurementUnit.label(): Int =
    when (this) {
        MeasurementUnit.UNIT -> R.string.unit_units
        MeasurementUnit.GRAM -> R.string.unit_grams
        MeasurementUnit.KILOGRAM -> R.string.unit_kilograms
        MeasurementUnit.MILLILITER -> R.string.unit_milliliters
        MeasurementUnit.LITER -> R.string.unit_liters
        MeasurementUnit.RATION -> R.string.unit_rations
    }

private tailrec fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
