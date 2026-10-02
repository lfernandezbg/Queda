@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming", "detekt:LongParameterList")

package com.luisete.queda.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.luisete.queda.core.designsystem.theme.QuedaSpacing

@Composable
fun SettingsScreen(
    householdName: String,
    syncText: String,
    inviteCode: String?,
    onInvite: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(QuedaSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(QuedaSpacing.Large)) {
                Text(
                    text = stringResource(R.string.settings_household),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = householdName,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        Button(onClick = onInvite) {
            Text(stringResource(R.string.settings_invite))
        }
        if (inviteCode != null) {
            Text(
                text = stringResource(R.string.settings_invitation, inviteCode),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onRefresh) {
            Text(stringResource(R.string.settings_sync) + ": " + syncText)
        }
        TextButton(onClick = onSignOut) {
            Text(stringResource(R.string.settings_sign_out))
        }
    }
}
