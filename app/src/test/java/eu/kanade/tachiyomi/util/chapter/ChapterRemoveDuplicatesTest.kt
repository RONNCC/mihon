package eu.kanade.tachiyomi.util.chapter

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ChapterRemoveDuplicatesTest {

    private fun chapter(id: Long, number: Double, scanlator: String? = null) =
        Chapter.create().copy(id = id, chapterNumber = number, scanlator = scanlator)

    @Test
    fun `keeps one chapter of neighbours with the same number`() {
        val chapters = listOf(
            chapter(1, 1.0, "A"),
            chapter(2, 1.0, "B"),
            chapter(3, 2.0, "A"),
            chapter(4, 2.0, "B"),
        )

        chapters.removeDuplicates(chapter(4, 2.0, "B")).map { it.id } shouldBe listOf(2L, 4L)
        chapters.removeDuplicates(chapter(1, 1.0, "A")).map { it.id } shouldBe listOf(1L, 3L)
    }

    @Test
    fun `keeps a number that comes again further on in its own place`() {
        // A second season numbered from 1 again, in source order
        val chapters = listOf(chapter(1, 1.0), chapter(2, 2.0), chapter(3, 1.0), chapter(4, 2.0))

        chapters.removeDuplicates(chapter(3, 1.0)).map { it.id } shouldBe listOf(1L, 2L, 3L, 4L)
    }

    @Test
    fun `keeps every chapter without a recognised number`() {
        val chapters = listOf(chapter(1, -1.0), chapter(2, -1.0), chapter(3, 1.0))

        chapters.removeDuplicates(chapter(3, 1.0)).map { it.id } shouldBe listOf(1L, 2L, 3L)
    }
}
