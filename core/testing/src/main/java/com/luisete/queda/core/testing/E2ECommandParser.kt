package com.luisete.queda.core.testing

import java.net.URI

object E2ECommandParser {
    private const val SCHEME = "queda-e2e"

    fun parse(uriString: String?): E2ECommand {
        if (uriString.isNullOrBlank()) return E2ECommand.Invalid("Null or blank URI")

        return try {
            val uri = URI.create(uriString)
            validateAndParse(uri)
        } catch (e: IllegalArgumentException) {
            E2ECommand.Invalid("Malformed URI: ${e.message}")
        }
    }

    private fun validateAndParse(uri: URI): E2ECommand {
        val validationError = getValidationError(uri)
        if (validationError != null) return validationError

        return when (val host = uri.host) {
            "reset" -> parseReset(uri)
            "seed" -> parseSeed(uri)
            else -> E2ECommand.Invalid("Unknown host: $host")
        }
    }

    private fun getValidationError(uri: URI): E2ECommand.Invalid? {
        return when {
            uri.scheme != SCHEME -> E2ECommand.Invalid("Invalid scheme")
            uri.query != null -> E2ECommand.Invalid("Query not supported")
            uri.fragment != null -> E2ECommand.Invalid("Fragment not supported")
            else -> null
        }
    }

    private fun parseReset(uri: URI): E2ECommand {
        return if (uri.path != "" && uri.path != "/") {
            E2ECommand.Invalid("Invalid path for reset")
        } else {
            E2ECommand.Reset
        }
    }

    private fun parseSeed(uri: URI): E2ECommand {
        return when (uri.path) {
            "/empty" -> E2ECommand.SeedEmpty
            else -> E2ECommand.Invalid("Unknown seed: ${uri.path}")
        }
    }
}
