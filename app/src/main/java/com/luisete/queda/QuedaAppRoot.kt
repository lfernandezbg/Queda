@file:Suppress("ktlint:standard:function-naming", "detekt:LongMethod", "detekt:FunctionNaming")

package com.luisete.queda

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luisete.queda.core.data.household.FirebaseHouseholdManager
import com.luisete.queda.core.data.household.HouseholdSyncStatus
import com.luisete.queda.core.designsystem.QuedaTestTags
import com.luisete.queda.feature.inventory.AddExactItemRoute
import com.luisete.queda.feature.inventory.AddExactItemViewModel
import com.luisete.queda.feature.inventory.BarcodeScannerRoute
import com.luisete.queda.feature.inventory.BarcodeScannerViewModel
import com.luisete.queda.feature.inventory.ContinuousScanViewModel
import com.luisete.queda.feature.inventory.InventoryRoute
import com.luisete.queda.feature.inventory.InventoryViewModel
import com.luisete.queda.feature.inventory.ProductLookupFeedback
import com.luisete.queda.feature.settings.SettingsScreen
import com.luisete.queda.feature.shopping.ShoppingRoute
import com.luisete.queda.feature.shopping.ShoppingViewModel
import com.luisete.queda.feature.today.TodayRoute
import com.luisete.queda.feature.today.TodayViewModel

@Suppress("FunctionNaming")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuedaAppRoot(
    manager: FirebaseHouseholdManager? = null,
    householdName: String = "",
    initialRoute: String = "inventory",
) {
    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId = true
                }
                .testTag(QuedaTestTags.APP_ROOT),
        color = MaterialTheme.colorScheme.background,
    ) {
        val navController = rememberNavController()
        val route = navController.currentBackStackEntryAsState().value?.destination?.route
        val status = manager?.syncStatus?.collectAsStateWithLifecycle()?.value
        val invite = manager?.inviteCode?.collectAsStateWithLifecycle()?.value
        Scaffold(
            topBar = {
                if (manager != null && route != "inventory/scanner") {
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                        Text(householdName, Modifier.weight(1f).padding(start = 16.dp, top = 16.dp))
                        Text(statusLabel(status), Modifier.padding(top = 16.dp))
                        TextButton(onClick = manager::refresh) { Text(stringResource(R.string.sync_refresh)) }
                    }
                }
            },
            bottomBar = {
                val fullScreenRoutes =
                    listOf(
                        "inventory/scanner",
                        "inventory/add-exact?barcode={barcode}&name={name}&lookup={lookup}",
                    )
                if (route !in fullScreenRoutes) {
                    NavigationBar {
                        listOf(
                            Triple("today", R.string.nav_today, Icons.Filled.Home),
                            Triple("inventory", R.string.nav_inventory, Icons.Filled.Inventory2),
                            Triple("scan", R.string.nav_scan, Icons.Filled.QrCodeScanner),
                            Triple("shopping", R.string.nav_shopping, Icons.Filled.ShoppingCart),
                            Triple("settings", R.string.nav_settings, Icons.Filled.MoreHoriz),
                        ).forEach { (destination, titleId, vector) ->
                            NavigationBarItem(
                                selected = route == destination,
                                onClick = {
                                    if (destination == "scan") {
                                        navController.navigate("inventory") { launchSingleTop = true }
                                        navController.navigate("inventory/scanner") { launchSingleTop = true }
                                    } else {
                                        navController.navigate(destination) {
                                            launchSingleTop = true
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(vector, contentDescription = stringResource(titleId)) },
                                label = { Text(stringResource(titleId)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            QuedaNavHost(
                navController = navController,
                initialRoute = initialRoute,
                manager = manager,
                householdName = householdName,
                status = status,
                invite = invite,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun statusLabel(status: HouseholdSyncStatus?): String =
    when (status) {
        HouseholdSyncStatus.UPDATED -> stringResource(R.string.sync_updated)
        HouseholdSyncStatus.PENDING -> stringResource(R.string.sync_pending)
        HouseholdSyncStatus.OFFLINE -> stringResource(R.string.sync_offline)
        HouseholdSyncStatus.ERROR -> stringResource(R.string.sync_error)
        null -> ""
    }

@Composable
@Suppress("FunctionNaming", "LongParameterList")
private fun QuedaNavHost(
    navController: NavHostController,
    initialRoute: String,
    manager: FirebaseHouseholdManager?,
    householdName: String,
    status: HouseholdSyncStatus?,
    invite: String?,
    modifier: Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = initialRoute,
        modifier = modifier.testTag(QuedaTestTags.SCREEN_FOUNDATION),
    ) {
        composable(route = "today") {
            TodayRoute(hiltViewModel<TodayViewModel>(), onShopping = {
                navController.navigate("shopping")
            }, onInventory = { navController.navigate("inventory") })
        }
        composable(route = "shopping") { ShoppingRoute(hiltViewModel<ShoppingViewModel>()) }
        composable(route = "settings") {
            SettingsScreen(
                householdName = householdName.ifBlank { "Mi Hogar" },
                syncText = statusLabel(status).ifBlank { stringResource(R.string.sync_updated) },
                inviteCode = invite,
                onInvite = { manager?.createInvite() },
                onRefresh = { manager?.refresh() },
                onSignOut = { manager?.signOut() },
            )
        }
        composable(route = "inventory") {
            InventoryRoute(
                viewModel = hiltViewModel(),
                onAddItem = {
                    navController.navigate("inventory/add-exact") {
                        launchSingleTop = true
                    }
                },
                onScanBarcode = {
                    navController.navigate("inventory/scanner") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = "inventory/add-exact?barcode={barcode}&name={name}&lookup={lookup}",
            arguments =
                listOf(
                    navArgument("barcode") { nullable = true },
                    navArgument("name") { nullable = true },
                    navArgument("lookup") { nullable = true },
                ),
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")
            val name = backStackEntry.arguments?.getString("name")
            val lookup = backStackEntry.arguments?.getString("lookup")
            val viewModel: AddExactItemViewModel = hiltViewModel()
            LaunchedEffect(barcode, name, lookup) {
                if (barcode != null) {
                    viewModel.onScannedProduct(
                        barcode,
                        name,
                        ProductLookupFeedback.valueOf(lookup ?: ProductLookupFeedback.NOT_FOUND.name),
                    )
                }
            }
            AddExactItemRoute(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack("inventory", inclusive = false)
                },
            )
        }
        composable(route = "inventory/scanner") { backStackEntry ->
            val inventoryEntry = remember(backStackEntry) { navController.getBackStackEntry("inventory") }
            val inventoryViewModel: InventoryViewModel = hiltViewModel(inventoryEntry)
            val scannerViewModel: BarcodeScannerViewModel = hiltViewModel()

            BarcodeScannerRoute(
                viewModel = scannerViewModel,
                continuousSession = if (manager != null) hiltViewModel<ContinuousScanViewModel>() else null,
                onBack = { navController.popBackStack() },
                onNavigateToAddItem = { event ->
                    val route =
                        "inventory/add-exact?barcode=${event.barcode}" +
                            "&name=${Uri.encode(event.suggestedName.orEmpty())}" +
                            "&lookup=${event.lookupFeedback.name}"
                    navController.navigate(route) {
                        popUpTo("inventory")
                    }
                },
                onNavigateToInventoryWithItem = { itemId ->
                    inventoryViewModel.selectItemById(itemId)
                    navController.popBackStack()
                },
            )
        }
    }
}
