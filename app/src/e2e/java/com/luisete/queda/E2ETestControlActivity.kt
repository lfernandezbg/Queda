@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:argument-list-wrapping",
    "ktlint:standard:import-ordering",
    "detekt:LongMethod",
    "detekt:FunctionNaming",
)

package com.luisete.queda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.core.domain.inventory.ExternalProductLookup
import com.luisete.queda.core.domain.inventory.ExternalProductResult
import com.luisete.queda.core.domain.inventory.ResolveScannedBarcodeUseCase
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.testing.E2ECommand
import com.luisete.queda.core.testing.E2ECommandParser
import com.luisete.queda.feature.inventory.AddExactItemRoute
import com.luisete.queda.feature.inventory.AddExactItemViewModel
import com.luisete.queda.feature.inventory.BarcodeScannerNavigationEvent
import com.luisete.queda.feature.inventory.BarcodeScannerScreen
import com.luisete.queda.feature.inventory.BarcodeScannerViewModel
import com.luisete.queda.feature.inventory.ContinuousScanViewModel
import com.luisete.queda.feature.inventory.InventoryRoute
import com.luisete.queda.feature.inventory.InventoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luisete.queda.feature.inventory.PermissionState
import com.luisete.queda.feature.inventory.ProductLookupFeedback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class E2ETestControlActivity : ComponentActivity() {
    @Inject
    lateinit var database: QuedaDatabase

    @Inject
    lateinit var resolveScannedBarcodeUseCase: ResolveScannedBarcodeUseCase

    private val currentScreen = MutableStateFlow<String>("app")
    private val resetCount = MutableStateFlow<Int>(0)
    private val scanCount = MutableStateFlow<Int>(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val screen by currentScreen.collectAsStateWithLifecycle()
            val resetId by resetCount.collectAsStateWithLifecycle()
            val scanId by scanCount.collectAsStateWithLifecycle()
            QuedaTheme {
                androidx.compose.material3.Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics {
                                testTagsAsResourceId = true
                            },
                ) {
                    androidx.activity.compose.BackHandler {
                        // Prevent back button / hideKeyboard from exiting E2ETestControlActivity
                    }
                    androidx.compose.runtime.key(resetId) {
                        when (screen) {
                            "shopping" -> {
                                val shoppingViewModel = hiltViewModel<com.luisete.queda.feature.shopping.ShoppingViewModel>()
                                com.luisete.queda.feature.shopping.ShoppingRoute(shoppingViewModel)
                            }
                            "today" -> {
                                val todayViewModel = hiltViewModel<com.luisete.queda.feature.today.TodayViewModel>()
                                com.luisete.queda.feature.today.TodayRoute(todayViewModel, onShopping = {}, onInventory = {})
                            }
                            "settings_preview" -> {
                                com.luisete.queda.feature.settings.SettingsScreen(
                                    householdName = "Mi Hogar",
                                    syncText = "Actualizado",
                                    inviteCode = "INVITE-1234-ABCD",
                                    onInvite = {},
                                    onRefresh = {},
                                    onSignOut = {},
                                )
                            }
                            "household_access_preview" -> {
                                E2EHouseholdAccessScreen()
                            }
                            "choose_household_preview" -> {
                                E2EChooseHouseholdScreen()
                            }
                            "scanner_preview" -> {
                                val scannerViewModel = hiltViewModel<com.luisete.queda.feature.inventory.BarcodeScannerViewModel>()
                                com.luisete.queda.feature.inventory.BarcodeScannerRoute(
                                    viewModel = scannerViewModel,
                                    onBack = { currentScreen.value = "app" },
                                    onNavigateToAddItem = {},
                                    onNavigateToInventoryWithItem = {},
                                )
                            }
                            "continuous" -> {
                                val barcode = intent.data?.getQueryParameter("barcode") ?: "3017620422003"
                                androidx.compose.runtime.key(barcode) {
                                    E2EScanHost(
                                        barcode,
                                        resolveScannedBarcodeUseCase,
                                        onExit = { currentScreen.value = "app" },
                                        continuous = true,
                                    )
                                }
                            }
                            "scan" -> {
                                val command = E2ECommandParser.parse(intent.dataString) as? E2ECommand.Scan
                                if (command != null) {
                                    androidx.compose.runtime.key(scanId) {
                                        E2EScanHost(
                                            command.barcode,
                                            resolveScannedBarcodeUseCase,
                                            onExit = { currentScreen.value = "app" },
                                            scanSessionId = scanId,
                                        )
                                    }
                                } else {
                                    QuedaAppRoot()
                                }
                            }
                            else -> QuedaAppRoot()
                        }
                    }
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.dataString
        if (intent.data?.host == "shopping") {
            currentScreen.value = "shopping"
            return
        }
        if (intent.data?.host == "today") {
            currentScreen.value = "today"
            return
        }
        if (intent.data?.host == "settings_preview") {
            currentScreen.value = "settings_preview"
            return
        }
        if (intent.data?.host == "household_access_preview") {
            currentScreen.value = "household_access_preview"
            return
        }
        if (intent.data?.host == "choose_household_preview") {
            currentScreen.value = "choose_household_preview"
            return
        }
        if (intent.data?.host == "scanner_preview") {
            currentScreen.value = "scanner_preview"
            return
        }
        if (intent.data?.host == "continuous") {
            currentScreen.value = "continuous"
            return
        }
        val command = E2ECommandParser.parse(uri)

        Log.d("E2E", "handleCommand: $command")

        if (command is E2ECommand.Reset || command is E2ECommand.SeedEmpty) {
            currentScreen.value = "app"
            viewModelStore.clear()
            runBlocking(Dispatchers.IO) {
                clearE2EData()
            }
            resetCount.value++
            return
        } else if (command is E2ECommand.Scan) {
            scanCount.value++
            currentScreen.value = "scan"
        }
    }

    private fun clearE2EData() {
        try {
            database.clearAllTables()
        } catch (e: Exception) {
            Log.w("E2E", "clearAllTables failed", e)
        }
        getSharedPreferences("queda_e2e_control", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

sealed class E2EScreen {
    data class Scanner(val barcode: String) : E2EScreen()

    data class AddItem(
        val barcode: String,
        val name: String?,
        val feedback: ProductLookupFeedback,
    ) : E2EScreen()

    data class Inventory(val itemId: String) : E2EScreen()
}

@Composable
@Suppress("ktlint:standard:function-naming")
fun E2EScanHost(
    initialBarcode: String,
    resolver: ResolveScannedBarcodeUseCase,
    onExit: () -> Unit,
    continuous: Boolean = false,
    scanSessionId: Int = 0,
) {
    var currentScreen by remember { mutableStateOf<E2EScreen>(E2EScreen.Scanner(initialBarcode)) }

    when (val screen = currentScreen) {
        is E2EScreen.Scanner -> {
            val factory = remember(resolver) { E2EBarcodeScannerFactory(resolver) }
            val viewModel: BarcodeScannerViewModel = viewModel(key = "e2e_scanner_${scanSessionId}_${screen.barcode}", factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val session = if (continuous) hiltViewModel<ContinuousScanViewModel>() else null
            val savedCount = session?.savedCount?.collectAsStateWithLifecycle()?.value ?: 0
            val saveError = session?.error?.collectAsStateWithLifecycle()?.value ?: false
            val saving = session?.saving?.collectAsStateWithLifecycle()?.value ?: false

            LaunchedEffect(Unit) {
                viewModel.setContinuousMode(continuous)
                viewModel.onPermissionStatusChanged(PermissionState.GRANTED)
                viewModel.onBarcodeDetected(screen.barcode)
            }

            LaunchedEffect(session) { session?.savedEvents?.collect { viewModel.resume() } }

            LaunchedEffect(viewModel) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is BarcodeScannerNavigationEvent.ToAddItem -> {
                            currentScreen =
                                E2EScreen.AddItem(
                                    event.barcode,
                                    event.suggestedName,
                                    event.lookupFeedback,
                                )
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
                continuousAvailable = continuous,
                savedCount = savedCount,
                saveError = saveError,
                isSaving = saving,
                onModeChange = viewModel::setContinuousMode,
                onSkip = viewModel::resume,
                onSave = { pending, name, amount, unit -> session?.save(pending, name, amount, unit) },
                cameraPreviewSlot = {
                    if (continuous) {
                        Box(Modifier.fillMaxSize()) {
                            Button(
                                onClick = { viewModel.onBarcodeDetected("8412345678905") },
                                modifier = Modifier.align(Alignment.Center),
                            ) {
                                Text("Leer siguiente código de prueba")
                            }
                        }
                    }
                },
            )
        }

        is E2EScreen.AddItem -> {
            val viewModel: AddExactItemViewModel = hiltViewModel()
            LaunchedEffect(screen.barcode) {
                viewModel.onScannedProduct(screen.barcode, screen.name, screen.feedback)
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

private class E2EBarcodeScannerFactory(
    private val resolver: ResolveScannedBarcodeUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == BarcodeScannerViewModel::class.java)
        val catalog =
            object : ExternalProductLookup {
                override suspend fun findByBarcode(barcode: Barcode): ExternalProductResult =
                    if (barcode.value == "3017620422003") {
                        ExternalProductResult.Found("Crema de cacao de prueba")
                    } else {
                        ExternalProductResult.NotFound
                    }
            }
        return BarcodeScannerViewModel(resolver, catalog) as T
    }
}

@Composable
fun E2EHouseholdAccessScreen() {
    var email by remember { mutableStateOf("familia@ejemplo.com") }
    var password by remember { mutableStateOf("••••••••") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(com.luisete.queda.core.designsystem.theme.QuedaSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(com.luisete.queda.core.designsystem.theme.QuedaSpacing.Medium),
    ) {
        Text(stringResource(R.string.household_welcome), style = MaterialTheme.typography.headlineSmall)
        com.luisete.queda.core.designsystem.component.QuedaTextField(
            email,
            { email = it },
            stringResource(R.string.household_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        com.luisete.queda.core.designsystem.component.QuedaTextField(
            password,
            { password = it },
            stringResource(R.string.household_password),
            visualTransformation = PasswordVisualTransformation(),
        )
        com.luisete.queda.core.designsystem.component.QuedaPrimaryButton(stringResource(R.string.household_sign_in), {})
        com.luisete.queda.core.designsystem.component.QuedaSecondaryButton(stringResource(R.string.household_register), {})
    }
}

@Composable
fun E2EChooseHouseholdScreen() {
    var name by remember { mutableStateOf("Mi Hogar") }
    var code by remember { mutableStateOf("a1b2c3d4e5f67890123456789abcdef0") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(com.luisete.queda.core.designsystem.theme.QuedaSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(com.luisete.queda.core.designsystem.theme.QuedaSpacing.Medium),
    ) {
        Text(stringResource(R.string.household_choose), style = MaterialTheme.typography.headlineSmall)
        com.luisete.queda.core.designsystem.component.QuedaTextField(name, { name = it }, stringResource(R.string.household_name))
        com.luisete.queda.core.designsystem.component.QuedaPrimaryButton(stringResource(R.string.household_create), {})
        Text(stringResource(R.string.household_join_explanation))
        com.luisete.queda.core.designsystem.component.QuedaTextField(code, { code = it }, stringResource(R.string.household_code))
        com.luisete.queda.core.designsystem.component.QuedaSecondaryButton(stringResource(R.string.household_join), {})
        com.luisete.queda.core.designsystem.component.QuedaSecondaryButton(stringResource(R.string.household_sign_out), {})
    }
}
