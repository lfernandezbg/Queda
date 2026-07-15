@file:Suppress("ktlint:standard:function-naming")

package com.luisete.queda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luisete.queda.core.designsystem.QuedaTestTags

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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(QuedaTestTags.SCREEN_FOUNDATION),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.foundation_message),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(24.dp),
            )
        }
    }
}
