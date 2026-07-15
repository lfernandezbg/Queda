package com.luisete.queda.core.testing

import java.nio.charset.StandardCharsets
import java.util.UUID

object DeterministicIds {
    /**
     * Produces a stable UUID based on a scenario name and an entity seed.
     */
    fun get(
        scenario: String,
        seed: String,
    ): String {
        return UUID.nameUUIDFromBytes(
            "$scenario\u0000$seed".toByteArray(StandardCharsets.UTF_8),
        ).toString()
    }
}
