package com.luisete.queda.core.database

sealed interface AddExactInventoryItemDbResult {
    data object Added : AddExactInventoryItemDbResult

    data object DuplicateProductName : AddExactInventoryItemDbResult
}
