package com.luisete.queda.core.domain.result

sealed interface DomainResult<out T>

data class Success<T>(
    val value: T,
) : DomainResult<T>

data class Failure(
    val error: DomainError,
) : DomainResult<Nothing>

sealed interface DomainError {
    data object IncompatibleQuantityDimensions :
        DomainError

    data object NegativeQuantity :
        DomainError

    data object TooManyDecimalPlaces :
        DomainError

    data object InsufficientQuantity :
        DomainError

    data object ApproximateLevelDidNotDecrease :
        DomainError

    data object ResultingQuantityMustBePositive :
        DomainError

    data object UnchangedQuantity :
        DomainError

    data object ProductNotFound :
        DomainError

    data object StorageFailure :
        DomainError

    data object AmountMustBePositive :
        DomainError

    data object AmountMustBeLowerThanCurrent :
        DomainError

    data object IncompatibleMode :
        DomainError
}
