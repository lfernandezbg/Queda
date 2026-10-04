package com.luisete.queda.feature.onboarding

import com.luisete.queda.core.domain.auth.AccessAccountUseCase
import com.luisete.queda.core.domain.auth.AccountAction
import com.luisete.queda.core.domain.auth.AccountRepository
import com.luisete.queda.core.domain.auth.AccountResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun rapidSubmissionsCannotCreateTwoAuthenticationRequests() =
        runTest {
            var requests = 0
            val complete = CompletableDeferred<AccountResult>()
            val repository =
                object : AccountRepository {
                    override suspend fun access(
                        action: AccountAction,
                        email: String,
                        password: String,
                    ): AccountResult {
                        requests++
                        return complete.await()
                    }
                }
            val model = AccountViewModel(AccessAccountUseCase(repository))
            model.email("user@example.com")
            model.password("password")
            model.submit(AccountAction.SIGN_IN)
            model.submit(AccountAction.REGISTER)
            advanceUntilIdle()
            assertEquals(1, requests)
            assertTrue(model.state.value.busy)
            complete.complete(AccountResult.AUTHENTICATED)
            advanceUntilIdle()
            assertEquals(Unit, model.events.first())
            assertEquals("", model.state.value.password)
            assertFalse(model.state.value.busy)
        }
}
