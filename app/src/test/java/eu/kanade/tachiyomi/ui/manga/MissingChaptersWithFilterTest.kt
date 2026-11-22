package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.download.model.Download
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

class MissingChaptersWithFilterTest {

    // Chapters 1-4 exist, but only the unread ones (1 and 4) pass the "unread" filter
    private val chapters = listOf(
        chapter(id = 1, number = 1.0, read = false),
        chapter(id = 2, number = 2.0, read = true),
        chapter(id = 3, number = 3.0, read = true),
        chapter(id = 4, number = 4.0, read = false),
    )

    @Test
    fun `chapters hidden by a filter are not reported missing`() {
        val state = state(manga(Manga.CHAPTER_SHOW_UNREAD))

        assertEquals(listOf(1L, 4L), state.processedChapters.map { it.id })
        assertEquals(0, state.missingChapterCount)
        assertTrue(state.chapterListItems.none { it is ChapterList.MissingCount })
    }

    @Test
    fun `real gaps are still reported without a filter`() {
        val state = state(manga(), chapters.filter { it.id != 2L && it.id != 3L })

        assertEquals(2, state.missingChapterCount)
        assertEquals(listOf(2), state.chapterListItems.filterIsInstance<ChapterList.MissingCount>().map { it.count })
    }

    private fun manga(filter: Long = 0L) = Manga.create().copy(
        id = 1,
        source = 2,
        title = "Manga",
        chapterFlags = filter or Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SORT_ASC,
    )

    private fun chapter(id: Long, number: Double, read: Boolean) =
        Chapter.create().copy(id = id, mangaId = 1, chapterNumber = number, read = read, url = "/$id")

    private fun state(manga: Manga, list: List<Chapter> = chapters) = MangaViewModel.State.Success(
        manga = manga,
        source = mockk(),
        sourceName = "source",
        isFromSource = false,
        chapters = list.map { ChapterList.Item(it, Download.State.NOT_DOWNLOADED) },
        availableScanlators = emptySet(),
        excludedScanlators = emptySet(),
    )
}
