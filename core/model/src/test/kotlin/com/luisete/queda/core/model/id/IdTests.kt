package com.luisete.queda.core.model.id

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("SwallowedException")
class IdTests {
    @Test
    fun `HouseholdId rejects empty and blank`() {
        assertRejectsBlank { HouseholdId.from(it) }
    }

    @Test
    fun `ProductId rejects empty and blank`() {
        assertRejectsBlank { ProductId.from(it) }
    }

    @Test
    fun `StockItemId rejects empty and blank`() {
        assertRejectsBlank { StockItemId.from(it) }
    }

    @Test
    fun `LocationId rejects empty and blank`() {
        assertRejectsBlank { LocationId.from(it) }
    }

    @Test
    fun `ShoppingEntryId rejects empty and blank`() {
        assertRejectsBlank { ShoppingEntryId.from(it) }
    }

    @Test
    fun `DomainEventId rejects empty and blank`() {
        assertRejectsBlank { DomainEventId.from(it) }
    }

    @Test
    fun `IDs generate unique and non-empty values`() {
        val id1 = HouseholdId.newId()
        val id2 = HouseholdId.newId()
        assertTrue(id1.value.isNotEmpty())
        assertNotEquals(id1, id2)
    }

    @Test
    fun `IDs preserve valid values`() {
        val value = "test-id"
        assertEquals(value, ProductId.from(value).value)
    }

    private fun assertRejectsBlank(factory: (String) -> Unit) {
        listOf("", " ", "  \n ").forEach { blank ->
            try {
                factory(blank)
                throw AssertionError("Should have rejected blank string: '$blank'")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }
}
