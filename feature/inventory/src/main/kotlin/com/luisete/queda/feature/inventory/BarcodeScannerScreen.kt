@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaIconButton
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaScaffold
import com.luisete.queda.core.designsystem.component.QuedaTopAppBar
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
@Suppress("LongMethod")
fun BarcodeScannerRoute(
    viewModel: BarcodeScannerViewModel,
    onBack: () -> Unit,
    onNavigateToAddItem: (String) -> Unit,
    onNavigateToInventoryWithItem: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hadPreviousCompletedRequest by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is BarcodeScannerNavigationEvent.ToAddItem -> onNavigateToAddItem(event.barcode)
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
