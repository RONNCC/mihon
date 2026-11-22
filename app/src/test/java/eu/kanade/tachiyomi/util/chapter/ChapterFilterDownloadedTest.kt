package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.download.DownloadCache
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

class ChapterFilterDownloadedTest {

    private val manga = Manga.create().copy(id = 1, source = 2, title = "Manga")
    private val chapters = (1L..3L).map { Chapter.create().copy(id = it, mangaId = manga.id, url = "/$it") }
    private val downloadCache = mockk<DownloadCache> {
        every { isChapterDownloaded(any(), any(), any(), any(), any()) } answers { thirdArg<String>() == "/1" }
    }

    @Test
    fun `keeps the downloaded chapters and the one to keep`() {
        val result = chapters.filterDownloaded(manga, downloadCache, keepChapterId = 3L)

        assertEquals(listOf(1L, 3L), result.map { it.id })
    }
}
