package com.luisete.queda.core.domain.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessAccountUseCaseTest {
    @Test
    fun invalidEmailCannotReachTheService() =
        runTest {
            val repository = RecordingAccountRepository()
            val result = AccessAccountUseCase(repository)(AccountAction.SIGN_IN, "bad@", "password")
            assertEquals(AccountResult.INVALID, result)
            assertEquals(0, repository.calls)
        }

    @Test
    fun recoveryNeedsEmailButNoPassword() =
        runTest {
            val repository = RecordingAccountRepository()
            val result = AccessAccountUseCase(repository)(AccountAction.RECOVER, " user@example.com ", "")
            assertEquals(AccountResult.RECOVERY_SENT, result)
            assertEquals("user@example.com", repository.email)
            assertEquals(1, repository.calls)
        }

    @Test
    fun signInDoesNotTrimPasswords() =
        runTest {
            val repository = RecordingAccountRepository()
            AccessAccountUseCase(repository)(AccountAction.SIGN_IN, "user@example.com", " password ")
            assertEquals(" password ", repository.password)
        }

    private class RecordingAccountRepository : AccountRepository {
        var calls = 0
        var email = ""
        var password = ""

        override suspend fun access(
            action: AccountAction,
            email: String,
            password: String,
        ): AccountResult {
            calls++
            this.email = email
            this.password = password
            return if (action == AccountAction.RECOVER) AccountResult.RECOVERY_SENT else AccountResult.AUTHENTICATED
        }
    }
}
