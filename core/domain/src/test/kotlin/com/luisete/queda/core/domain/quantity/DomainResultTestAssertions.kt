package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.domain.result.Failure
import com.luisete.queda.core.domain.result.Success

internal fun <T> DomainResult<T>.successValue(): T =
    when (this) {
        is Success -> value
        is Failure ->
            throw AssertionError(
                "Expected Success but was Failure: $error",
            )
    }

internal fun DomainResult<*>.failureError(): DomainError =
    when (this) {
        is Success ->
            throw AssertionError(
                "Expected Failure but was Success: $value",
            )

        is Failure -> error
    }
