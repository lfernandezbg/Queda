package com.luisete.queda.core.domain.inventory

sealed interface AddExactItemRepositoryResult {
    data object Added : AddExactItemRepositoryResult

    data object DuplicateProductName : AddExactItemRepositoryResult

    data object StorageFailure : AddExactItemRepositoryResult
}
