@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luisete.queda.core.designsystem.QuedaTestTags
import com.luisete.queda.feature.inventory.AddExactItemRoute
import com.luisete.queda.feature.inventory.AddExactItemViewModel
import com.luisete.queda.feature.inventory.BarcodeScannerRoute
import com.luisete.queda.feature.inventory.BarcodeScannerViewModel
import com.luisete.queda.feature.inventory.InventoryRoute
import com.luisete.queda.feature.inventory.InventoryViewModel

@Suppress("FunctionNaming")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuedaAppRoot() {
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
        QuedaNavHost(
            navController = navController,
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun QuedaNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "inventory",
        modifier = Modifier.testTag(QuedaTestTags.SCREEN_FOUNDATION),
    ) {
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
            route = "inventory/add-exact?barcode={barcode}",
            arguments = listOf(navArgument("barcode") { nullable = true }),
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")
            val viewModel: AddExactItemViewModel = hiltViewModel()
            LaunchedEffect(barcode) {
                if (barcode != null) {
                    viewModel.onBarcodeAssociated(barcode)
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
                onBack = { navController.popBackStack() },
                onNavigateToAddItem = { barcode ->
                    navController.navigate("inventory/add-exact?barcode=$barcode") {
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
