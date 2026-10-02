@file:Suppress(
    "ktlint:standard:function-naming",
    "detekt:FunctionNaming",
    "detekt:LongMethod",
    "detekt:CyclomaticComplexMethod",
    "detekt:MaxLineLength",
)

package com.luisete.queda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.data.household.FirebaseHouseholdManager
import com.luisete.queda.core.data.household.HouseholdSession
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.theme.QuedaSpacing

@Composable
fun HouseholdEntry(manager: FirebaseHouseholdManager) {
    val session by manager.state.collectAsStateWithLifecycle()
    val message by manager.message.collectAsStateWithLifecycle()
    val syncError by manager.syncError.collectAsStateWithLifecycle()
    val conflictItem by manager.conflictItem.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        when (val current = session) {
            HouseholdSession.SignedOut -> {
                HouseholdAccess(manager, message)
            }
            HouseholdSession.Loading -> Text(stringResource(R.string.household_loading), Modifier.padding(QuedaSpacing.Large))
            HouseholdSession.ChooseHousehold -> {
                ChooseHousehold(manager, message)
            }
            HouseholdSession.LoadFailed -> {
                Column(
                    Modifier.padding(QuedaSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
                ) {
                    Text(message ?: stringResource(R.string.household_loading_failed))
                    QuedaPrimaryButton(stringResource(R.string.sync_refresh), manager::retry)
                    QuedaSecondaryButton(stringResource(R.string.household_sign_out), manager::signOut)
                }
            }
            is HouseholdSession.Active -> {
                if (syncError != null) {
                    Text(
                        syncError.orEmpty(),
                        Modifier.padding(horizontal = QuedaSpacing.Medium),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (conflictItem != null) {
                    QuedaSecondaryButton(
                        stringResource(R.string.sync_resolve),
                        { showDiscardDialog = true },
                        Modifier.padding(horizontal = QuedaSpacing.Small),
                    )
                }
                if (showDiscardDialog && conflictItem != null) {
                    AlertDialog(
                        onDismissRequest = { showDiscardDialog = false },
                        title = { Text(stringResource(R.string.sync_conflict_title)) },
                        text = { Text(stringResource(R.string.sync_conflict_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                manager.discardConflict()
                                showDiscardDialog = false
                            }) { Text(stringResource(R.string.sync_keep_remote)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDiscardDialog = false }) {
                                Text(stringResource(R.string.sync_cancel))
                            }
                        },
                    )
                }
                if (message != null) Text(message.orEmpty(), Modifier.padding(QuedaSpacing.Small), color = MaterialTheme.colorScheme.error)
                Box(Modifier.weight(1f)) { key(current.id) { QuedaAppRoot(manager, current.name) } }
            }
        }
    }
}

@Composable
private fun HouseholdAccess(
    manager: FirebaseHouseholdManager,
    message: String?,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(QuedaSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
    ) {
        Text(stringResource(R.string.household_welcome), style = MaterialTheme.typography.headlineSmall)
        QuedaTextField(
            email,
            { email = it },
            stringResource(R.string.household_email),
            modifier = Modifier.testTag("household_auth_email"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        QuedaTextField(
            password,
            { password = it },
            stringResource(R.string.household_password),
            modifier = Modifier.testTag("household_auth_password"),
            visualTransformation = PasswordVisualTransformation(),
        )
        QuedaPrimaryButton(
            stringResource(R.string.household_sign_in),
            { manager.signIn(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank(),
        )
        QuedaSecondaryButton(
            stringResource(R.string.household_register),
            { manager.register(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank(),
        )
        if (message != null) Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ChooseHousehold(
    manager: FirebaseHouseholdManager,
    message: String?,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(QuedaSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
    ) {
        Text(stringResource(R.string.household_choose), style = MaterialTheme.typography.headlineSmall)
        QuedaTextField(name, { name = it }, stringResource(R.string.household_name), Modifier.testTag("household_name"))
        QuedaPrimaryButton(
            stringResource(R.string.household_create),
            { manager.createHousehold(name) },
            enabled = name.trim().length in 2..60,
        )
        Text(stringResource(R.string.household_join_explanation))
        QuedaTextField(
            code,
            { code = it.trim().lowercase() },
            stringResource(R.string.household_code),
            Modifier.testTag("household_join_code"),
        )
        QuedaSecondaryButton(stringResource(R.string.household_join), { manager.joinHousehold(code) }, enabled = code.length == 32)
        QuedaSecondaryButton(stringResource(R.string.household_sign_out), manager::signOut)
        if (message != null) Text(message, color = MaterialTheme.colorScheme.error)
    }
}
