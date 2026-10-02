package com.luisete.queda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import com.luisete.queda.feature.shopping.ShoppingRoute
import com.luisete.queda.feature.shopping.ShoppingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class E2EShoppingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuedaTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true },
                ) {
                    ShoppingRoute(hiltViewModel<ShoppingViewModel>())
                }
            }
        }
    }
}
