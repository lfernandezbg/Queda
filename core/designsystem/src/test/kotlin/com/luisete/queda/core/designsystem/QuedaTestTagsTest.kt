package com.luisete.queda.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuedaTestTagsTest {
    @Test
    fun `test tags should not have duplicates`() {
        assertEquals(
            "Found duplicate test tags",
            QuedaTestTags.staticTags.size,
            QuedaTestTags.staticTags.distinct().size,
        )
    }

    @Test
    fun `deliberate duplicate check`() {
        val listWithDuplicates = listOf("tag1", "tag2", "tag1")
        assertNotEquals(
            "Validation function should detect duplicates",
            listWithDuplicates.size,
            listWithDuplicates.distinct().size,
        )
    }

    @Test
    fun `test tags should not be empty`() {
        QuedaTestTags.staticTags.forEach { tag ->
            assertTrue("Test tag should not be empty", tag.isNotEmpty())
        }
    }

    @Test
    fun `test tags should follow regex`() {
        val regex = Regex("^[a-z][a-z0-9_]*$")
        QuedaTestTags.staticTags.forEach { tag ->
            assertTrue("Test tag '$tag' does not match pattern", regex.matches(tag))
        }
    }
}
