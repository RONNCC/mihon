package eu.kanade.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelectionRangeToTest {

    private val items = (0..9).toList()

    @Test
    fun `extends the selection down to an item below it`() {
        assertEquals(listOf(4, 5, 6), items.selectionRangeTo(7) { it in setOf(2, 3) }.toList())
    }

    @Test
    fun `fills the gap above and below the selection`() {
        val selected = setOf(4, 5)
        assertEquals(listOf(1, 2, 3), items.selectionRangeTo(0) { it in selected }.toList())
        assertEquals(listOf(6, 7, 8), items.selectionRangeTo(9) { it in selected }.toList())
    }

    @Test
    fun `is empty inside the selection or without one`() {
        assertEquals(emptyList<Int>(), items.selectionRangeTo(5) { it in setOf(3, 8) }.toList())
        assertEquals(emptyList<Int>(), items.selectionRangeTo(5) { false }.toList())
    }

    @Test
    fun `ignores selected ids that are no longer in the list`() {
        // The list shrank: ids selected before are gone, so no stale position can index past the end
        val shrunk = listOf(0, 1, 2)
        assertEquals(listOf(1), shrunk.selectionRangeTo(2) { it == 0 || it == 100 }.toList())
    }
}
