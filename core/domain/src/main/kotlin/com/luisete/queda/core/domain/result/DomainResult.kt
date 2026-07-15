package com.luisete.queda.core.domain.result

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>

    data class Failure(val error: DomainError) : DomainResult<Nothing>

    fun <R> map(transform: (T) -> R): DomainResult<R> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> Failure(error)
        }
}

sealed interface DomainError {
    data object IncompatibleQuantityDimensions : DomainError

    data object NegativeQuantity : DomainError

    data object TooManyDecimalPlaces : DomainError

    data object InsufficientQuantity : DomainError

    data object ApproximateLevelDidNotDecrease : DomainError

    data object InvalidQuantityText : DomainError
}
