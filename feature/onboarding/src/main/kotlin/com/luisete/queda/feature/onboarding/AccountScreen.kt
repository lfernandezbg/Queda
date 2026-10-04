@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.component.QuedaPrimaryButton
import com.luisete.queda.core.designsystem.component.QuedaSecondaryButton
import com.luisete.queda.core.designsystem.component.QuedaTextField
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.domain.auth.AccountAction
import com.luisete.queda.core.domain.auth.AccountResult

@Composable
fun AccountRoute(
    viewModel: AccountViewModel,
    onAuthenticated: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { onAuthenticated() } }
    AccountScreen(state, viewModel::email, viewModel::password, viewModel::submit, viewModel::mode)
}

@Composable
fun AccountScreen(
    state: AccountState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: (AccountAction) -> Unit,
    onMode: (AccountAction) -> Unit = {},
) {
    var showPassword by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                .padding(QuedaSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium)) {
                Text(
                    stringResource(
                        when (state.mode) {
                            AccountAction.SIGN_IN -> R.string.account_welcome
                            AccountAction.REGISTER -> R.string.account_register
                            AccountAction.RECOVER -> R.string.account_recover
                        },
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                )
                QuedaTextField(
                    state.email,
                    onEmail,
                    stringResource(R.string.account_email),
                    Modifier.testTag("household_auth_email"),
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                if (state.mode != AccountAction.RECOVER) {
                    QuedaTextField(
                        state.password,
                        onPassword,
                        stringResource(R.string.account_password),
                        Modifier.testTag("household_auth_password"),
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation =
                            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                val label =
                                    if (showPassword) R.string.account_hide_password else R.string.account_show_password
                                Text(stringResource(label))
                            }
                        },
                    )
                }
                AccountActions(state, onSubmit, onMode)
            }
        }
    }
}

@Composable
private fun AccountActions(
    state: AccountState,
    onSubmit: (AccountAction) -> Unit,
    onMode: (AccountAction) -> Unit,
) {
    QuedaPrimaryButton(
        stringResource(
            if (state.busy) {
                R.string.account_wait
            } else {
                when (state.mode) {
                    AccountAction.SIGN_IN -> R.string.account_enter
                    AccountAction.REGISTER -> R.string.account_register
                    AccountAction.RECOVER -> R.string.account_send_reset
                }
            },
        ),
        { onSubmit(state.mode) },
        Modifier.fillMaxWidth(),
        enabled =
            !state.busy && state.email.isNotBlank() &&
                (state.mode == AccountAction.RECOVER || state.password.isNotBlank()),
    )
    QuedaSecondaryButton(
        stringResource(if (state.mode == AccountAction.SIGN_IN) R.string.account_register else R.string.account_back),
        { onMode(if (state.mode == AccountAction.SIGN_IN) AccountAction.REGISTER else AccountAction.SIGN_IN) },
        Modifier.fillMaxWidth(),
        enabled = !state.busy,
    )
    if (state.mode == AccountAction.SIGN_IN) {
        QuedaSecondaryButton(
            stringResource(R.string.account_recover),
            { onMode(AccountAction.RECOVER) },
            Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
    }
    state.result?.let { AccountResultMessage(it) }
}

@Composable
private fun AccountResultMessage(result: AccountResult) {
    Text(
        stringResource(
            when (result) {
                AccountResult.RECOVERY_SENT -> R.string.account_recovery_sent
                AccountResult.INVALID -> R.string.account_invalid
                AccountResult.REJECTED -> R.string.account_rejected
                AccountResult.UNAVAILABLE -> R.string.account_unavailable
                AccountResult.AUTHENTICATED -> R.string.account_wait
            },
        ),
        color =
            if (result == AccountResult.RECOVERY_SENT) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
    )
}
