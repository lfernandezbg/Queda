package com.luisete.queda.core.domain.auth

import javax.inject.Inject

enum class AccountAction { SIGN_IN, REGISTER, RECOVER }

enum class AccountResult { AUTHENTICATED, RECOVERY_SENT, INVALID, REJECTED, UNAVAILABLE }

interface AccountRepository {
    suspend fun access(
        action: AccountAction,
        email: String,
        password: String,
    ): AccountResult
}

class AccessAccountUseCase
    @Inject
    constructor(private val repository: AccountRepository) {
        suspend operator fun invoke(
            action: AccountAction,
            email: String,
            password: String,
        ): AccountResult {
            val address = email.trim()
            val valid =
                address.length <= MAX_EMAIL_LENGTH && EMAIL.matches(address) &&
                    (action == AccountAction.RECOVER || password.isNotBlank())
            return if (valid) repository.access(action, address, password) else AccountResult.INVALID
        }

        companion object {
            private const val MAX_EMAIL_LENGTH = 254
            private val EMAIL = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
        }
    }
