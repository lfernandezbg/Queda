@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luisete.queda.core.designsystem.QuedaTestTags
import com.luisete.queda.feature.inventory.AddExactItemRoute
import com.luisete.queda.feature.inventory.InventoryRoute

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

        NavHost(
            navController = navController,
            startDestination = "inventory",
            modifier = Modifier.testTag(QuedaTestTags.SCREEN_FOUNDATION),
        ) {
            composable("inventory") {
                InventoryRoute(
                    viewModel = hiltViewModel(),
                    onAddItem = { navController.navigate("inventory/add-exact") },
                )
            }
            composable("inventory/add-exact") {
                AddExactItemRoute(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
