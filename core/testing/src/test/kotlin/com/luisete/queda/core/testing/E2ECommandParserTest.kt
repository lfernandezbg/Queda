package com.luisete.queda.core.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E2ECommandParserTest {
    @Test
    fun `parse reset command`() {
        val result = E2ECommandParser.parse("queda-e2e://reset")
        assertEquals(E2ECommand.Reset, result)
    }

    @Test
    fun `parse seed empty command`() {
        val result = E2ECommandParser.parse("queda-e2e://seed/empty")
        assertEquals(E2ECommand.SeedEmpty, result)
    }

    @Test
    fun `parse null URI`() {
        val result = E2ECommandParser.parse(null)
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse empty URI`() {
        val result = E2ECommandParser.parse("")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse incorrect scheme`() {
        val result = E2ECommandParser.parse("not-queda-e2e://reset")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse incorrect host`() {
        val result = E2ECommandParser.parse("queda-e2e://wronghost")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse incorrect path for reset`() {
        val result = E2ECommandParser.parse("queda-e2e://reset/something")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse incorrect path for seed`() {
        val result = E2ECommandParser.parse("queda-e2e://seed/wrong")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse with query`() {
        val result = E2ECommandParser.parse("queda-e2e://reset?param=val")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse with fragment`() {
        val result = E2ECommandParser.parse("queda-e2e://reset#frag")
        assertTrue(result is E2ECommand.Invalid)
    }

    @Test
    fun `parse malformed URI`() {
        val result = E2ECommandParser.parse("!!!://malformed")
        assertTrue(result is E2ECommand.Invalid)
    }
}
