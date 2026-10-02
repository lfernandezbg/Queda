package com.luisete.queda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.luisete.queda.core.designsystem.theme.QuedaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class E2EInventoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuedaTheme { QuedaAppRoot() } }
    }
}
