package tachiyomi.domain.history.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga

class GetNextChaptersTest {

    private val manga = Manga.create().copy(id = 1, chapterFlags = Manga.CHAPTER_SORTING_NUMBER)
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val getManga = mockk<GetManga>()
    private val getNextChapters = GetNextChapters(getChaptersByMangaId, getManga, mockk())

    @Test
    fun `Chapters after a read chapter are next`() = runTest {
        givenChapters(chapter(1, read = true), chapter(2), chapter(3))

        getNextChapters.await(1, fromChapterId = 1, onlyUnread = false).map { it.id } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `Unfinished chapter is next itself`() = runTest {
        givenChapters(chapter(1, read = true), chapter(2), chapter(3))

        getNextChapters.await(1, fromChapterId = 2, onlyUnread = false).map { it.id } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `No chapter is dropped when the one to resume from is hidden`() = runTest {
        givenChapters(chapter(1), chapter(2), chapter(3))

        getNextChapters.await(1, fromChapterId = 99, onlyUnread = false).map { it.id } shouldBe listOf(1L, 2L, 3L)
    }

    private fun givenChapters(vararg chapters: Chapter) {
        coEvery { getManga.await(1) } returns manga
        coEvery { getChaptersByMangaId.await(1, applyScanlatorFilter = true) } returns chapters.toList()
    }

    private fun chapter(id: Long, read: Boolean = false) = Chapter.create().copy(
        id = id,
        mangaId = 1,
        chapterNumber = id.toDouble(),
        read = read,
    )
}
