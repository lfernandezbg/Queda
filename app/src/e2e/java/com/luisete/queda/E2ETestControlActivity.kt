package com.luisete.queda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.testing.E2ECommand
import com.luisete.queda.core.testing.E2ECommandParser
import com.luisete.queda.feature.inventory.AddExactItemRoute
import com.luisete.queda.feature.inventory.AddExactItemViewModel
import com.luisete.queda.feature.inventory.BarcodeScannerNavigationEvent
import com.luisete.queda.feature.inventory.BarcodeScannerScreen
import com.luisete.queda.feature.inventory.BarcodeScannerViewModel
import com.luisete.queda.feature.inventory.InventoryRoute
import com.luisete.queda.feature.inventory.InventoryViewModel
import com.luisete.queda.feature.inventory.PermissionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class E2ETestControlActivity : ComponentActivity() {
    @Inject
    lateinit var database: QuedaDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent.dataString
        val command = E2ECommandParser.parse(uri)

        Log.d("E2E", "handleCommand: $command")

        if (command is E2ECommand.Reset || command is E2ECommand.SeedEmpty) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    clearE2EData()
                }
                startActivity(
                    Intent(this@E2ETestControlActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                )
                finish()
            }
        } else if (command is E2ECommand.Scan) {
            setContent {
                QuedaTheme {
                    E2EScanHost(command.barcode, onExit = { finish() })
                }
            }
        } else {
            finish()
        }
    }

    private fun clearE2EData() {
        database.clearAllTables()
        getSharedPreferences("queda_e2e_control", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

sealed class E2EScreen {
    data class Scanner(val barcode: String) : E2EScreen()

    data class AddItem(val barcode: String) : E2EScreen()

    data class Inventory(val itemId: String) : E2EScreen()
}

@Composable
@Suppress("ktlint:standard:function-naming")
fun E2EScanHost(
    initialBarcode: String,
    onExit: () -> Unit,
) {
    var currentScreen by remember { mutableStateOf<E2EScreen>(E2EScreen.Scanner(initialBarcode)) }

    when (val screen = currentScreen) {
        is E2EScreen.Scanner -> {
            val viewModel: BarcodeScannerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onPermissionStatusChanged(PermissionState.GRANTED)
                viewModel.onBarcodeDetected(screen.barcode)
            }

            LaunchedEffect(viewModel) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is BarcodeScannerNavigationEvent.ToAddItem -> {
                            currentScreen = E2EScreen.AddItem(event.barcode)
                        }

                        is BarcodeScannerNavigationEvent.ToInventoryWithItem -> {
                            currentScreen = E2EScreen.Inventory(event.itemId)
                        }
                    }
                }
            }

            BarcodeScannerScreen(
                uiState = uiState,
                onBack = onExit,
                onRetryPermission = {},
                onOpenSettings = {},
                cameraPreviewSlot = {
                    // Deterministic fake camera content slot
                },
            )
        }

        is E2EScreen.AddItem -> {
            val viewModel: AddExactItemViewModel = hiltViewModel()
            LaunchedEffect(screen.barcode) {
                viewModel.onBarcodeAssociated(screen.barcode)
            }
            AddExactItemRoute(
                viewModel = viewModel,
                onBack = onExit,
            )
        }

        is E2EScreen.Inventory -> {
            val viewModel: InventoryViewModel = hiltViewModel()
            LaunchedEffect(screen.itemId) {
                viewModel.selectItemById(screen.itemId)
            }
            InventoryRoute(
                viewModel = viewModel,
                onAddItem = {},
                onScanBarcode = {},
            )
        }
    }
}
