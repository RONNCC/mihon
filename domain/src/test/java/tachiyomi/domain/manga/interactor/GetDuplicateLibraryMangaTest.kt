package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaTitle
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.repository.MangaRepository

class GetDuplicateLibraryMangaTest {

    private val repository = mockk<MangaRepository> {
        coEvery { getFavoriteTitles() } returns listOf(
            MangaTitle(1, 1, "Ёлки-палки"),
            MangaTitle(2, 1, "Café Society"),
            MangaTitle(3, 1, "100% Perfect Girl"),
            MangaTitle(4, 1, "100 Perfect Girl"),
            MangaTitle(5, 1, "Same Entry"),
        )
        // Echoes the ids it was asked for
        coEvery { getDuplicateLibraryManga(any(), any()) } answers {
            secondArg<List<Long>>().map { MangaWithChapterCount(Manga.create().copy(id = it), 0) }
        }
    }
    private val getDuplicateLibraryManga = GetDuplicateLibraryManga(repository)

    @Test
    fun `Title match ignores the case of non ASCII letters`() = runTest {
        findDuplicates("ЁЛКИ-ПАЛКИ") shouldBe listOf(1L)
        findDuplicates("café") shouldBe listOf(2L)
    }

    @Test
    fun `Percent in a title is not a wildcard`() = runTest {
        findDuplicates("100% Perfect") shouldBe listOf(3L)
        findDuplicates("1%t") shouldBe emptyList<Long>()
    }

    @Test
    fun `Blank title matches nothing and the entry itself is skipped`() = runTest {
        findDuplicates("  ") shouldBe emptyList<Long>()
        findDuplicates("same entry", id = 5) shouldBe emptyList<Long>()
    }

    private suspend fun findDuplicates(title: String, id: Long = 99): List<Long> {
        return getDuplicateLibraryManga(Manga.create().copy(id = id, title = title)).map { it.manga.id }
    }
}
