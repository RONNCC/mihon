package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import kotlin.time.Instant

class ShouldUpdateDbChapterTest {

    private val shouldUpdate = ShouldUpdateDbChapter()

    private val stored = Chapter.create().copy(
        name = "Chapter 1",
        scanlator = "Group",
        dateUpload = Instant.fromEpochMilliseconds(1_000),
    )

    @Test
    fun `identical chapter is not changed`() {
        shouldUpdate.await(stored, stored.copy()) shouldBe false
    }

    @Test
    fun `source without upload date does not change a stored date`() {
        shouldUpdate.await(stored, stored.copy(dateUpload = null)) shouldBe false
    }

    @Test
    fun `source with another upload date is a change`() {
        shouldUpdate.await(stored, stored.copy(dateUpload = Instant.fromEpochMilliseconds(2_000))) shouldBe true
    }

    @Test
    fun `dropped scanlator is a change`() {
        shouldUpdate.await(stored, stored.copy(scanlator = null)) shouldBe true
    }
}
