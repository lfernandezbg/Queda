package com.luisete.queda.core.data.auth

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.luisete.queda.core.data.household.awaitTask
import com.luisete.queda.core.domain.auth.AccountAction
import com.luisete.queda.core.domain.auth.AccountRepository
import com.luisete.queda.core.domain.auth.AccountResult
import javax.inject.Inject

class FirebaseAccountRepository
    @Inject
    constructor(private val auth: FirebaseAuth) : AccountRepository {
        override suspend fun access(
            action: AccountAction,
            email: String,
            password: String,
        ): AccountResult =
            try {
                when (action) {
                    AccountAction.SIGN_IN -> {
                        auth.signInWithEmailAndPassword(email, password).awaitTask()
                        AccountResult.AUTHENTICATED
                    }
                    AccountAction.REGISTER -> {
                        auth.createUserWithEmailAndPassword(email, password).awaitTask()
                        AccountResult.AUTHENTICATED
                    }
                    AccountAction.RECOVER -> {
                        auth.sendPasswordResetEmail(email).awaitTask()
                        AccountResult.RECOVERY_SENT
                    }
                }
            } catch (_: FirebaseNetworkException) {
                AccountResult.UNAVAILABLE
            } catch (error: FirebaseAuthInvalidUserException) {
                if (action == AccountAction.RECOVER && error.errorCode == "ERROR_USER_NOT_FOUND") {
                    AccountResult.RECOVERY_SENT
                } else {
                    AccountResult.REJECTED
                }
            } catch (_: FirebaseException) {
                AccountResult.REJECTED
            }
    }
