package eu.kanade.tachiyomi.data.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StaleChapterFilesTest {

    @Test
    fun `keeps the files of the current pages and the chapter metadata`() {
        val names = listOf("001.jpg", "002.png", "003.tmp", "003__001.jpg", ".nomedia", "ComicInfo.xml")

        assertEquals(emptyList<String>(), staleChapterFileNames(names, pageCount = 3))
    }

    @Test
    fun `flags the pages past a shorter page list and duplicate writes`() {
        val names = listOf("001.jpg", "002.jpg", "003.jpg", "003 (1).jpg", "004.tmp", "extra.bin")

        assertEquals(
            listOf("003.jpg", "003 (1).jpg", "004.tmp", "extra.bin"),
            staleChapterFileNames(names, pageCount = 2),
        )
    }

    @Test
    fun `pads page numbers to the width of the page count`() {
        assertEquals("007", pageFilePrefix(7, pageCount = 12))
        assertEquals("0007", pageFilePrefix(7, pageCount = 1200))
        assertEquals(listOf("001.jpg"), staleChapterFileNames(listOf("0001.jpg", "001.jpg"), pageCount = 1200))
    }
}
