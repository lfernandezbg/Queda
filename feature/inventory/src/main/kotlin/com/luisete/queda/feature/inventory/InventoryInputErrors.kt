package com.luisete.queda.feature.inventory

enum class NameInputError {
    BLANK,
    TOO_LONG,
    FORBIDDEN_CHARACTER,
}

enum class QuantityInputError {
    BLANK,
    INVALID_FORMAT,
    NOT_POSITIVE,
    TOO_MANY_DECIMALS,
}
