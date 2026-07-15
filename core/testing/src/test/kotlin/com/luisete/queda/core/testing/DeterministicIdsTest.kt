package com.luisete.queda.core.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class DeterministicIdsTest {
    @Test
    fun `same scenario and seed produce same ID`() {
        val id1 = DeterministicIds.get("BASIC", "prod1")
        val id2 = DeterministicIds.get("BASIC", "prod1")
        assertEquals(id1, id2)
    }

    @Test
    fun `different seeds produce different IDs`() {
        val id1 = DeterministicIds.get("BASIC", "prod1")
        val id2 = DeterministicIds.get("BASIC", "prod2")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `different scenarios produce different IDs for same seed`() {
        val id1 = DeterministicIds.get("SCENARIO_A", "prod1")
        val id2 = DeterministicIds.get("SCENARIO_B", "prod1")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `result is a valid UUID`() {
        val id = DeterministicIds.get("TEST", "123")
        assertNotNull(UUID.fromString(id))
    }
}
