package com.luisete.queda.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.auth.AccessAccountUseCase
import com.luisete.queda.core.domain.auth.AccountAction
import com.luisete.queda.core.domain.auth.AccountResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class AccountState(
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val result: AccountResult? = null,
    val mode: AccountAction = AccountAction.SIGN_IN,
)

@HiltViewModel
class AccountViewModel
    @Inject
    constructor(private val access: AccessAccountUseCase) : ViewModel() {
        private val mutableState = MutableStateFlow(AccountState())
        val state = mutableState.asStateFlow()
        private val gate = AtomicBoolean(false)
        private val authenticated = Channel<Unit>(Channel.BUFFERED)
        val events = authenticated.receiveAsFlow()

        fun email(value: String) {
            if (!gate.get()) mutableState.update { it.copy(email = value, result = null) }
        }

        fun password(value: String) {
            if (!gate.get()) mutableState.update { it.copy(password = value, result = null) }
        }

        fun mode(value: AccountAction) {
            if (!gate.get()) mutableState.update { it.copy(mode = value, result = null) }
        }

        fun submit(action: AccountAction) {
            if (!gate.compareAndSet(false, true)) return
            val snapshot = mutableState.value
            mutableState.update { it.copy(busy = true, result = null) }
            viewModelScope.launch {
                try {
                    val result = access(action, snapshot.email, snapshot.password)
                    mutableState.update { it.copy(result = result) }
                    if (result == AccountResult.AUTHENTICATED) {
                        mutableState.update { it.copy(password = "") }
                        authenticated.send(Unit)
                    }
                } finally {
                    gate.set(false)
                    mutableState.update { it.copy(busy = false) }
                }
            }
        }
    }
