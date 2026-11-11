package app.wault

import app.wault.data.Reorder
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    @Test fun moveDown() = assertEquals(listOf("b", "a", "c"), Reorder.move(listOf("a", "b", "c"), 0, 1))
    @Test fun moveUp() = assertEquals(listOf("c", "a", "b"), Reorder.move(listOf("a", "b", "c"), 2, 0))
    @Test fun moveOutOfRangeIsNoop() = assertEquals(listOf("a", "b"), Reorder.move(listOf("a", "b"), 5, 0))
    @Test fun applyOrderKeepsUnknownAtEnd() {
        val out = Reorder.applyOrder(listOf("a", "b", "c", "d"), listOf("c", "a")) { it }
        assertEquals(listOf("c", "a", "b", "d"), out)
    }
}
