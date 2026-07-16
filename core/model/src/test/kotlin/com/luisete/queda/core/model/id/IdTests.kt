package com.luisete.queda.core.model.id

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IdTests {
    // HouseholdId
    @Test
    fun householdIdFromRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { HouseholdId.from("") }
    }

    @Test
    fun householdIdFromRejectsSingleSpace() {
        assertThrows(IllegalArgumentException::class.java) { HouseholdId.from(" ") }
    }

    @Test
    fun householdIdFromRejectsWhitespaceCombination() {
        assertThrows(IllegalArgumentException::class.java) { HouseholdId.from(" \t\n ") }
    }

    @Test
    fun householdIdFromPreservesValidValueExactly() {
        val v = "household-1"
        assertEquals(v, HouseholdId.from(v).value)
    }

    @Test
    fun householdIdNewIdReturnsNonBlankValue() {
        assertTrue(HouseholdId.newId().value.isNotBlank())
    }

    @Test
    fun householdIdConsecutiveNewIdsAreDifferent() {
        assertNotEquals(HouseholdId.newId(), HouseholdId.newId())
    }

    // ProductId
    @Test
    fun productIdFromRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { ProductId.from("") }
    }

    @Test
    fun productIdFromRejectsSingleSpace() {
        assertThrows(IllegalArgumentException::class.java) { ProductId.from(" ") }
    }

    @Test
    fun productIdFromRejectsWhitespaceCombination() {
        assertThrows(IllegalArgumentException::class.java) { ProductId.from(" \t\n ") }
    }

    @Test
    fun productIdFromPreservesValidValueExactly() {
        val v = "product-1"
        assertEquals(v, ProductId.from(v).value)
    }

    @Test
    fun productIdNewIdReturnsNonBlankValue() {
        assertTrue(ProductId.newId().value.isNotBlank())
    }

    @Test
    fun productIdConsecutiveNewIdsAreDifferent() {
        assertNotEquals(ProductId.newId(), ProductId.newId())
    }

    // StockItemId
    @Test
    fun stockItemIdFromRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { StockItemId.from("") }
    }

    @Test
    fun stockItemIdFromRejectsSingleSpace() {
        assertThrows(IllegalArgumentException::class.java) { StockItemId.from(" ") }
    }

    @Test
    fun stockItemIdFromRejectsWhitespaceCombination() {
        assertThrows(IllegalArgumentException::class.java) { StockItemId.from(" \t\n ") }
    }

    @Test
    fun stockItemIdFromPreservesValidValueExactly() {
        val v = "stock-1"
        assertEquals(v, StockItemId.from(v).value)
    }

    @Test
    fun stockItemIdNewIdReturnsNonBlankValue() {
        assertTrue(StockItemId.newId().value.isNotBlank())
    }

    @Test
    fun stockItemIdConsecutiveNewIdsAreDifferent() {
        assertNotEquals(StockItemId.newId(), StockItemId.newId())
    }

    // LocationId
    @Test
    fun locationIdFromRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { LocationId.from("") }
    }

    @Test
    fun locationIdFromRejectsSingleSpace() {
        assertThrows(IllegalArgumentException::class.java) { LocationId.from(" ") }
    }

    @Test
    fun locationIdFromRejectsWhitespaceCombination() {
        assertThrows(IllegalArgumentException::class.java) { LocationId.from(" \t\n ") }
    }

    @Test
    fun locationIdFromPreservesValidValueExactly() {
        val v = "location-1"
        assertEquals(v, LocationId.from(v).value)
    }

    @Test
    fun locationIdNewIdReturnsNonBlankValue() {
        assertTrue(LocationId.newId().value.isNotBlank())
    }

    @Test
    fun locationIdConsecutiveNewIdsAreDifferent() {
        assertNotEquals(LocationId.newId(), LocationId.newId())
    }

    // ShoppingEntryId
    @Test
    fun shoppingEntryIdFromRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { ShoppingEntryId.from("") }
    }

    @Test
    fun shoppingEntryIdFromRejectsSingleSpace() {
        assertThrows(IllegalArgumentException::class.java) { ShoppingEntryId.from(" ") }
    }

    @Test
    fun shoppingEntryIdFromRejectsWhitespaceCombination() {
        assertThrows(IllegalArgumentException::class.java) { ShoppingEntryId.from(" \t\n ") }
    }

    @Test
    fun shoppingEntryIdFromPreservesValidValueExactly() {
        val v = "shopping-1"
        assertEquals(v, ShoppingEntryId.from(v).value)
    }

    @Test
    fun shoppingEntryIdNewIdReturnsNonBlankValue() {
        assertTrue(ShoppingEntryId.newId().value.isNotBlank())
    }

    @Test
    fun shoppingEntryIdConsecutiveNewIdsAreDifferent() {
        assertNotEquals(ShoppingEntryId.newId(), ShoppingEntryId.newId())
    }

    // DomainEventId
    @Test
    fun domainEventIdFromRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { DomainEventId.from("") }
    }

    @Test
    fun domainEventIdFromRejectsSingleSpace() {
        assertThrows(IllegalArgumentException::class.java) { DomainEventId.from(" ") }
    }

    @Test
    fun domainEventIdFromRejectsWhitespaceCombination() {
        assertThrows(IllegalArgumentException::class.java) { DomainEventId.from(" \t\n ") }
    }

    @Test
    fun domainEventIdFromPreservesValidValueExactly() {
        val v = "event-1"
        assertEquals(v, DomainEventId.from(v).value)
    }

    @Test
    fun domainEventIdNewIdReturnsNonBlankValue() {
        assertTrue(DomainEventId.newId().value.isNotBlank())
    }

    @Test
    fun domainEventIdConsecutiveNewIdsAreDifferent() {
        assertNotEquals(DomainEventId.newId(), DomainEventId.newId())
    }
}
