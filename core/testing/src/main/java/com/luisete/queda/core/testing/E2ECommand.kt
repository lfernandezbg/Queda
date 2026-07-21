package com.luisete.queda.core.testing

sealed interface E2ECommand {
    data object Reset : E2ECommand

    data object SeedEmpty : E2ECommand

    data class Scan(val barcode: String) : E2ECommand

    data class Invalid(val reason: String) : E2ECommand
}
