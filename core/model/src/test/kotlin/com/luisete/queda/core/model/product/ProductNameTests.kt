package com.luisete.queda.core.model.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductNameTests {
    @Test
    fun emptyNameIsRejected() {
        assertEquals(ProductNameCreationResult.Blank, ProductName.create(""))
    }

    @Test
    fun spacesOnlyNameIsRejected() {
        assertEquals(ProductNameCreationResult.Blank, ProductName.create("   "))
    }

    @Test
    fun tabIsRejected() {
        assertEquals(ProductNameCreationResult.ContainsForbiddenCharacter, ProductName.create("Name\t"))
    }

    @Test
    fun newlineIsRejected() {
        assertEquals(ProductNameCreationResult.ContainsForbiddenCharacter, ProductName.create("Name\n"))
    }

    @Test
    fun carriageReturnIsRejected() {
        assertEquals(ProductNameCreationResult.ContainsForbiddenCharacter, ProductName.create("Name\r"))
    }

    @Test
    fun isoControlCharacterIsRejected() {
        assertEquals(ProductNameCreationResult.ContainsForbiddenCharacter, ProductName.create("Name" + 0.toChar()))
    }

    @Test
    fun unicodeFormatCharacterIsRejected() {
        assertEquals(ProductNameCreationResult.ContainsForbiddenCharacter, ProductName.create("Name" + '\u200B'))
    }

    @Test
    fun surroundingSpacesAreRemoved() {
        val res = ProductName.create("  Name  ") as ProductNameCreationResult.Success
        assertEquals("Name", res.productName.displayValue)
    }

    @Test
    fun repeatedAsciiSpacesAreCollapsed() {
        val res = ProductName.create("Name   Space") as ProductNameCreationResult.Success
        assertEquals("Name Space", res.productName.displayValue)
    }

    @Test
    fun originalLetterCaseIsPreserved() {
        val res = ProductName.create("iPhone") as ProductNameCreationResult.Success
        assertEquals("iPhone", res.productName.displayValue)
    }

    @Test
    fun normalizedKeyUsesLocaleRootLowercase() {
        val res = ProductName.create("iPhone") as ProductNameCreationResult.Success
        assertEquals("iphone", res.productName.normalizedKey)
    }

    @Test
    fun accentedCharactersArePreserved() {
        val res = ProductName.create("Café") as ProductNameCreationResult.Success
        assertEquals("Café", res.productName.displayValue)
        assertEquals("café", res.productName.normalizedKey)
    }

    @Test
    fun apostropheHyphenAndNumbersAreAccepted() {
        val name = "L\'Oréal-123"
        val res = ProductName.create(name) as ProductNameCreationResult.Success
        assertEquals(name, res.productName.displayValue)
    }

    @Test
    fun eightyCharactersAreAccepted() {
        val name = "a".repeat(80)
        assertTrue(ProductName.create(name) is ProductNameCreationResult.Success)
    }

    @Test
    fun eightyOneCharactersAreRejected() {
        val name = "a".repeat(81)
        assertEquals(ProductNameCreationResult.TooLong, ProductName.create(name))
    }

    @Test
    fun caseVariantsHaveSameNormalizedKey() {
        val res1 = ProductName.create("LECHE") as ProductNameCreationResult.Success
        val res2 = ProductName.create("leche") as ProductNameCreationResult.Success
        assertEquals(res1.productName.normalizedKey, res2.productName.normalizedKey)
    }

    @Test
    fun spacingVariantsHaveSameNormalizedKey() {
        val res1 = ProductName.create("Leche Entera") as ProductNameCreationResult.Success
        val res2 = ProductName.create("  Leche   Entera  ") as ProductNameCreationResult.Success
        assertEquals(res1.productName.normalizedKey, res2.productName.normalizedKey)
    }

    @Test
    fun ideographicSpaceIsNotTrimmedAsAsciiSpace() {
        // U+3000 is an ideographic space, it should not be trimmed by the ASCII space trimmer
        val ideographicSpace = "\u3000"
        val name = "${ideographicSpace}Milk$ideographicSpace"
        val res = ProductName.create(name) as ProductNameCreationResult.Success
        assertEquals(name, res.productName.displayValue)
    }
}
