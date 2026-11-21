package eu.kanade.tachiyomi.util.chapter

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

class ChapterToDeleteAfterReadTest {

    private val manga = Manga.create().copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER)

    private fun chapter(id: Long, number: Double, scanlator: String? = null) =
        Chapter.create().copy(id = id, chapterNumber = number, scanlator = scanlator, read = true)

    // Unsorted on purpose; the DB returns them in source order
    private val chapters = listOf(chapter(5, 5.0), chapter(3, 3.0), chapter(4, 4.0), chapter(1, 1.0), chapter(2, 2.0))

    @Test
    fun `picks the chapter the slots before the current one`() {
        chapters.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 2, skipDuplicates = false)?.id shouldBe 3
        chapters.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 0, skipDuplicates = false)?.id shouldBe 5
    }

    @Test
    fun `counts unread chapters in the position`() {
        val unread = chapters.map { if (it.id == 4L) it.copy(read = false) else it }

        unread.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 1, skipDuplicates = false)
            ?.read shouldBe false
        unread.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 2, skipDuplicates = false)
            ?.id shouldBe 3
    }

    @Test
    fun `returns null before the first chapter`() {
        chapters.getChapterToDeleteAfterRead(manga, chapter(2, 2.0), slots = 2, skipDuplicates = false) shouldBe null
    }

    @Test
    fun `uses the current chapter's own read state`() {
        // The database row of the chapter being finished is only written after the deletion is decided
        val stored = chapters.map { if (it.id == 5L) it.copy(read = false) else it }

        stored.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 0, skipDuplicates = false)
            ?.read shouldBe true
    }

    @Test
    fun `finds the position of a chapter a scanlator filter leaves out`() {
        val filtered = chapters.filterNot { it.id == 4L }

        filtered.getChapterToDeleteAfterRead(manga, chapter(4, 4.0), slots = 0, skipDuplicates = false)
            ?.id shouldBe 4
        filtered.getChapterToDeleteAfterRead(manga, chapter(4, 4.0), slots = 1, skipDuplicates = false)
            ?.id shouldBe 3
    }

    @Test
    fun `counts duplicates once only when skipping them`() {
        val withDupes = chapters + chapter(6, 4.0, scanlator = "B")

        withDupes.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 2, skipDuplicates = true)
            ?.id shouldBe 3
        withDupes.getChapterToDeleteAfterRead(manga, chapter(5, 5.0), slots = 2, skipDuplicates = false)
            ?.id shouldBe 4
    }
}
