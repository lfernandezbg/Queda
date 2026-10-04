@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming", "detekt:LongParameterList")

package com.luisete.queda.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    householdName: String,
    syncText: String,
    inviteCode: String?,
    onInvite: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    canInvite: Boolean = true,
    busy: Boolean = false,
    inviteExpiry: Long? = null,
    onLocations: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(QuedaSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(QuedaSpacing.Large)) {
                Text(stringResource(R.string.settings_household), style = MaterialTheme.typography.labelMedium)
                Text(householdName, style = MaterialTheme.typography.headlineMedium)
            }
        }
        onLocations?.let {
            QuedaSecondaryButton(stringResource(R.string.settings_locations), it, Modifier.fillMaxWidth())
        }
        if (canInvite) {
            QuedaPrimaryButton(stringResource(R.string.settings_invite), onInvite, enabled = !busy)
        } else {
            Text(stringResource(R.string.settings_owner_invites))
        }
        if (inviteCode != null) InvitationActions(householdName, inviteCode, inviteExpiry)
        QuedaSecondaryButton(stringResource(R.string.settings_sync) + ": " + syncText, onRefresh)
        QuedaSecondaryButton(stringResource(R.string.settings_sign_out), onSignOut, enabled = !busy)
    }
}

@Composable
private fun InvitationActions(
    householdName: String,
    code: String,
    expiry: Long?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    val instructions = stringResource(R.string.settings_invite_instructions)
    val expires = expiry?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
    val message =
        stringResource(R.string.settings_invite_message, householdName, code) + "\n" + instructions +
            (expires?.let { "\n" + stringResource(R.string.settings_invite_expiry, it) } ?: "")
    SelectionContainer { Text(code, Modifier.testTag("invitation_code")) }
    Text(instructions)
    if (expires != null) Text(stringResource(R.string.settings_invite_expiry, expires))
    QuedaSecondaryButton(stringResource(R.string.settings_copy), {
        clipboard.setText(AnnotatedString(message))
        copied = true
    })
    if (copied) Text(stringResource(R.string.settings_copied))
    QuedaSecondaryButton(stringResource(R.string.settings_share), {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                },
                null,
            ),
        )
    })
    QuedaSecondaryButton(stringResource(R.string.settings_email), {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:?body=" + Uri.encode(message)))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    },
                    null,
                ),
            )
        }
    })
}
