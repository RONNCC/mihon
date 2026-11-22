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
    fun `All chapters are next when the one to resume from does not exist`() = runTest {
        givenChapters(chapter(1), chapter(2), chapter(3))

        getNextChapters.await(1, fromChapterId = 99, onlyUnread = false).map { it.id } shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun `Unread hidden chapter resumes at the shown chapter of the same number`() = runTest {
        givenChapters(chapter(1), chapter(2), chapter(3), hidden = listOf(chapter(20, number = 2.0)))

        getNextChapters.await(1, fromChapterId = 20, onlyUnread = false).map { it.id } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `Read hidden chapter resumes at the next shown chapter`() = runTest {
        givenChapters(
            chapter(1),
            chapter(2),
            chapter(3),
            hidden = listOf(chapter(20, number = 2.0, read = true)),
        )

        getNextChapters.await(1, fromChapterId = 20, onlyUnread = false).map { it.id } shouldBe listOf(3L)
    }

    @Test
    fun `Nothing is next when the hidden chapter is after every shown chapter`() = runTest {
        givenChapters(chapter(1), chapter(2), hidden = listOf(chapter(20, number = 5.0, read = true)))

        getNextChapters.await(1, fromChapterId = 20, onlyUnread = false) shouldBe emptyList()
    }

    private fun givenChapters(vararg chapters: Chapter, hidden: List<Chapter> = emptyList()) {
        coEvery { getManga.await(1) } returns manga
        coEvery { getChaptersByMangaId.await(1, applyScanlatorFilter = true) } returns chapters.toList()
        coEvery { getChaptersByMangaId.await(1, applyScanlatorFilter = false) } returns chapters.toList() + hidden
    }

    private fun chapter(id: Long, read: Boolean = false, number: Double = id.toDouble()) = Chapter.create().copy(
        id = id,
        mangaId = 1,
        chapterNumber = number,
        read = read,
    )
}
