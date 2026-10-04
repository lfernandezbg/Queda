@file:Suppress(
    "ktlint:standard:function-naming",
    "detekt:FunctionNaming",
    "detekt:LongParameterList",
    "detekt:LongMethod",
    "detekt:CyclomaticComplexMethod",
    "detekt:MaxLineLength",
)

package com.luisete.queda.feature.inventory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaChoiceChip
import com.luisete.queda.core.designsystem.component.QuedaIconButton
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.model.quantity.MeasurementUnit
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
@Suppress("LongMethod")
fun BarcodeScannerRoute(
    viewModel: BarcodeScannerViewModel,
    onBack: () -> Unit,
    onNavigateToAddItem: (BarcodeScannerNavigationEvent.ToAddItem) -> Unit,
    onNavigateToInventoryWithItem: (String) -> Unit,
    continuousSession: ContinuousScanViewModel? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hadPreviousCompletedRequest by rememberSaveable { mutableStateOf(false) }
    val savedCount = continuousSession?.savedCount?.collectAsStateWithLifecycle()?.value ?: 0
    val saveError = continuousSession?.error?.collectAsStateWithLifecycle()?.value ?: false
    val isSaving = continuousSession?.saving?.collectAsStateWithLifecycle()?.value ?: false

    LaunchedEffect(continuousSession) {
        continuousSession?.savedEvents?.collect { viewModel.resume() }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is BarcodeScannerNavigationEvent.ToAddItem -> onNavigateToAddItem(event)
                is BarcodeScannerNavigationEvent.ToInventoryWithItem -> {
                    onNavigateToInventoryWithItem(event.itemId)
                }
            }
        }
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            val activity = context as? Activity
            val state =
                resolvePermissionState(
                    isGranted = isGranted,
                    shouldShowRationale =
                        activity?.let {
                            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                        } ?: false,
                    hadPreviousCompletedRequest = hadPreviousCompletedRequest,
                )
            hadPreviousCompletedRequest = true
            viewModel.onPermissionStatusChanged(state)
        }

    val requestPermission = {
        viewModel.onPermissionStatusChanged(PermissionState.REQUESTING)
        launcher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(uiState.permissionState) {
        if (uiState.permissionState == PermissionState.NOT_REQUESTED) {
            val currentStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (currentStatus == PackageManager.PERMISSION_GRANTED) {
                viewModel.onPermissionStatusChanged(PermissionState.GRANTED)
            } else {
                requestPermission()
            }
        }
    }

    val currentPermissionState by rememberUpdatedState(uiState.permissionState)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (
                        (currentPermissionState == PermissionState.DENIED) ||
                        (currentPermissionState == PermissionState.PERMANENTLY_DENIED)
                    ) {
                        val currentStatus =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (currentStatus == PackageManager.PERMISSION_GRANTED) {
                            viewModel.onPermissionStatusChanged(PermissionState.GRANTED)
                        }
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BarcodeScannerScreen(
        uiState = uiState,
        onBack = onBack,
        continuousAvailable = continuousSession != null,
        savedCount = savedCount,
        saveError = saveError,
        isSaving = isSaving,
        onModeChange = viewModel::setContinuousMode,
        onLotSelected = viewModel::selectLot,
        onSkip = viewModel::resume,
        onSave = { pending, name, quantity, unit -> continuousSession?.save(pending, name, quantity, unit) },
        onRetryPermission = requestPermission,
        onOpenSettings = {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            context.startActivity(intent)
        },
        cameraPreviewSlot = {
            CameraPreview(
                onBarcodeDetected = viewModel::onBarcodeDetected,
                isProcessing = uiState.isProcessing,
            )
        },
    )
}

internal fun resolvePermissionState(
    isGranted: Boolean,
    shouldShowRationale: Boolean,
    hadPreviousCompletedRequest: Boolean,
): PermissionState {
    return when {
        isGranted -> PermissionState.GRANTED
        shouldShowRationale -> PermissionState.DENIED
        hadPreviousCompletedRequest -> PermissionState.PERMANENTLY_DENIED
        else -> PermissionState.DENIED
    }
}

@Composable
@Suppress("LongMethod")
fun BarcodeScannerScreen(
    uiState: BarcodeScannerUiState,
    onBack: () -> Unit,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    cameraPreviewSlot: @Composable () -> Unit,
    continuousAvailable: Boolean = false,
    savedCount: Int = 0,
    saveError: Boolean = false,
    isSaving: Boolean = false,
    onModeChange: (Boolean) -> Unit = {},
    onLotSelected: (String) -> Unit = {},
    onSkip: () -> Unit = {},
    onSave: (PendingScan, String, String, MeasurementUnit) -> Unit = { _, _, _, _ -> },
) {
    QuedaScaffold(
        modifier = Modifier.testTag(InventoryTestTags.BARCODE_SCANNER_SCREEN),
        topBar = {
            ScannerTopBar(onBack)
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            ScannerContent(
                uiState = uiState,
                onRetryPermission = onRetryPermission,
                onOpenSettings = onOpenSettings,
                cameraPreviewSlot = cameraPreviewSlot,
            )
            if (uiState.permissionState == PermissionState.GRANTED && continuousAvailable) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(QuedaSpacing.Medium),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(QuedaSpacing.Medium)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(QuedaSpacing.Small)) {
                            QuedaChoiceChip(
                                stringResource(R.string.scan_single),
                                !uiState.continuousMode,
                                { onModeChange(false) },
                                Modifier.testTag("scan_mode_single"),
                            )
                            QuedaChoiceChip(
                                stringResource(R.string.scan_continuous),
                                uiState.continuousMode,
                                { onModeChange(true) },
                                Modifier.testTag("scan_mode_continuous"),
                            )
                        }
                        Text(stringResource(if (uiState.continuousMode) R.string.scan_continuous_help else R.string.scan_single_help))
                        if (uiState.continuousMode) {
                            Text(
                                pluralStringResource(
                                    R.plurals.scan_saved_count,
                                    savedCount,
                                    savedCount,
                                ),
                            )
                        }
                    }
                }
                uiState.pendingScan?.let { pending ->
                    ContinuousReview(pending, saveError, isSaving, onSave, onSkip)
                }
            }
        }
    }
    if (uiState.lotChoices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onSkip,
            title = { Text(stringResource(R.string.scan_choose_lot)) },
            text = {
                Column {
                    uiState.lotChoices.forEach { lot ->
                        OutlinedButton(
                            onClick = { onLotSelected(lot.stockItemId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(lot.name + " · " + lot.description) }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = onSkip) { Text(stringResource(R.string.scan_skip)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinuousReview(
    pending: PendingScan,
    error: Boolean,
    isSaving: Boolean,
    onSave: (PendingScan, String, String, MeasurementUnit) -> Unit,
    onSkip: () -> Unit,
) {
    var name by remember(pending) { mutableStateOf((pending as? PendingScan.New)?.suggestedName.orEmpty()) }
    var amount by remember(pending) { mutableStateOf("1") }
    var unit by remember(pending) { mutableStateOf(MeasurementUnit.UNIT) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onSkip() },
        sheetState = sheetState,
        modifier =
            Modifier
                .testTag("continuous_scan_review")
                .semantics { testTagsAsResourceId = true },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .semantics { testTagsAsResourceId = true }
                .padding(QuedaSpacing.Large),
        ) {
            Text(stringResource(R.string.scan_review_title), style = MaterialTheme.typography.headlineSmall)
            if (pending is PendingScan.New) {
                Text(stringResource(R.string.add_exact_item_barcode_associated, pending.barcode))
                OutlinedTextField(name, {
                    name = it
                }, label = {
                    Text(
                        stringResource(R.string.add_exact_item_name_label),
                    )
                }, modifier = Modifier.fillMaxWidth().testTag("continuous_scan_name"))
            } else if (pending is PendingScan.Existing) {
                Text(stringResource(R.string.scan_existing, pending.name))
            }
            if (pending !is PendingScan.Existing || !pending.isPresence) {
                OutlinedTextField(amount, {
                    amount = it
                }, label = {
                    Text(
                        stringResource(R.string.quantity_amount_label),
                    )
                }, modifier = Modifier.fillMaxWidth().testTag("continuous_scan_quantity"))
                Row {
                    listOf(
                        MeasurementUnit.UNIT,
                        MeasurementUnit.GRAM,
                        MeasurementUnit.KILOGRAM,
                        MeasurementUnit.MILLILITER,
                        MeasurementUnit.LITER,
                        MeasurementUnit.RATION,
                    ).forEach {
                            option ->
                        val label =
                            when (option) {
                                MeasurementUnit.UNIT -> R.string.unit_abbreviation_unit
                                MeasurementUnit.GRAM -> R.string.unit_abbreviation_gram
                                MeasurementUnit.KILOGRAM -> R.string.unit_abbreviation_kilogram
                                MeasurementUnit.MILLILITER -> R.string.unit_abbreviation_milliliter
                                MeasurementUnit.LITER -> R.string.unit_abbreviation_liter
                                MeasurementUnit.RATION -> R.string.unit_abbreviation_ration
                            }
                        OutlinedButton(
                            onClick = { unit = option },
                            enabled = unit != option,
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            if (error) Text(stringResource(R.string.scan_save_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                onSave(pending, name, amount, unit)
            }, enabled = !isSaving, modifier = Modifier.fillMaxWidth().testTag("continuous_scan_save")) {
                Text(stringResource(R.string.scan_save_continue))
            }
            OutlinedButton(
                onClick = onSkip,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.scan_skip)) }
        }
    }
}

@Composable
private fun ScannerTopBar(onBack: () -> Unit) {
    QuedaTopAppBar(
        title = stringResource(R.string.barcode_scanner_title),
        navigationIcon = {
            QuedaIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                modifier = Modifier.testTag(InventoryTestTags.BARCODE_SCANNER_CLOSE_BUTTON),
            )
        },
    )
}

@Composable
private fun ScannerContent(
    uiState: BarcodeScannerUiState,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    cameraPreviewSlot: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.permissionState) {
            PermissionState.GRANTED -> {
                cameraPreviewSlot()

                if (uiState.isProcessing) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.testTag(InventoryTestTags.BARCODE_SCANNER_LOOKUP_PROGRESS),
                        )
                        Spacer(modifier = Modifier.height(QuedaSpacing.Small))
                        Text(
                            text = stringResource(R.string.barcode_scanner_looking_up),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                uiState.lastError?.let { error ->
                    ErrorMessage(
                        error = error,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            PermissionState.REQUESTING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("barcode_scanner_requesting_progress"),
                    )
                }
            }

            PermissionState.DENIED -> {
                PermissionDeniedContent(
                    onRetry = onRetryPermission,
                )
            }

            PermissionState.PERMANENTLY_DENIED -> {
                PermissionPermanentlyDeniedContent(
                    onOpenSettings = onOpenSettings,
                )
            }

            else -> {}
        }
    }
}

internal class CameraResourceCoordinator(
    onBarcodeDetected: (String) -> Unit,
) : AutoCloseable {
    private val isDisposed = AtomicBoolean(false)
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    val analyzer = BarcodeAnalyzer(onBarcodeDetected)
    val imageAnalysis: ImageAnalysis =
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, analyzer)
            }
    var cameraProvider: ProcessCameraProvider? = null
        private set

    fun bind(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        if (isDisposed.get()) return

        val provider = cameraProvider ?: return

        val preview =
            Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        } catch (e: IllegalArgumentException) {
            Log.e("CameraCoordinator", "Invalid camera configuration", e)
        } catch (e: IllegalStateException) {
            Log.e("CameraCoordinator", "Camera binding failed", e)
        }
    }

    fun onProviderAvailable(provider: ProcessCameraProvider) {
        if (!isDisposed.get()) {
            cameraProvider = provider
        }
    }

    override fun close() {
        if (isDisposed.compareAndSet(false, true)) {
            imageAnalysis.clearAnalyzer()
            cameraProvider?.unbindAll()
            analyzer.close()
            executor.shutdown()
        }
    }
}

@Composable
fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    isProcessing: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentIsProcessing by rememberUpdatedState(isProcessing)

    val coordinator =
        remember {
            CameraResourceCoordinator { barcode ->
                if (!currentIsProcessing) {
                    currentOnBarcodeDetected(barcode)
                }
            }
        }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener(
                {
                    try {
                        val provider = cameraProviderFuture.get()
                        coordinator.onProviderAvailable(provider)
                        coordinator.bind(lifecycleOwner, previewView.surfaceProvider)
                    } catch (e: ExecutionException) {
                        Log.e("CameraPreview", "Failed to get camera provider", e)
                    } catch (e: InterruptedException) {
                        Log.e("CameraPreview", "Interrupted while getting camera provider", e)
                        Thread.currentThread().interrupt()
                    }
                },
                ContextCompat.getMainExecutor(ctx),
            )

            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(Unit) {
        onDispose {
            coordinator.close()
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRetry: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(QuedaSpacing.Large)
                .testTag(InventoryTestTags.BARCODE_SCANNER_PERMISSION_DENIED),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.barcode_scanner_permission_explanation),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(QuedaSpacing.Medium))
        QuedaPrimaryButton(
            text = stringResource(R.string.barcode_scanner_permission_retry),
            onClick = onRetry,
            modifier = Modifier.testTag(InventoryTestTags.BARCODE_SCANNER_PERMISSION_RETRY_BUTTON),
        )
    }
}

@Composable
private fun PermissionPermanentlyDeniedContent(onOpenSettings: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(QuedaSpacing.Large)
                .testTag(InventoryTestTags.BARCODE_SCANNER_PERMISSION_PERMANENTLY_DENIED),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.barcode_scanner_permission_permanently_denied),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(QuedaSpacing.Medium))
        QuedaPrimaryButton(
            text = stringResource(R.string.barcode_scanner_open_settings),
            onClick = onOpenSettings,
            modifier = Modifier.testTag(InventoryTestTags.BARCODE_SCANNER_OPEN_SETTINGS_BUTTON),
        )
    }
}

@Composable
private fun ErrorMessage(
    error: BarcodeScannerError,
    modifier: Modifier = Modifier,
) {
    val message =
        when (error) {
            BarcodeScannerError.INVALID_CHECK_DIGIT -> stringResource(R.string.barcode_error_invalid_check_digit)
            BarcodeScannerError.UNSUPPORTED_FORMAT -> stringResource(R.string.barcode_error_unsupported_format)
            BarcodeScannerError.NON_DIGIT -> stringResource(R.string.barcode_error_non_digit)
            BarcodeScannerError.STORAGE_FAILURE -> stringResource(R.string.barcode_error_storage_failure)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(QuedaSpacing.Medium)
                .testTag(InventoryTestTags.BARCODE_SCANNER_ERROR_MESSAGE),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
