package tachiyomi.domain.history.interactor

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover

class GetHistoryTest {

    private val getHistory = GetHistory(
        mockk {
            every { getHistory() } returns flowOf(
                listOf(
                    entry(1, "Ёлки-палки"),
                    entry(2, "Café Society"),
                    entry(3, "100% Perfect Girl"),
                    entry(4, "100 Perfect Girl"),
                    entry(5, "Snake_Eyes"),
                ),
            )
        },
    )

    @Test
    fun `Search ignores the case of non ASCII letters`() = runTest {
        search("ёлки") shouldBe listOf(1L)
        search("CAFÉ") shouldBe listOf(2L)
    }

    @Test
    fun `Search treats percent and underscore literally`() = runTest {
        search("100%") shouldBe listOf(3L)
        search("S_ake") shouldBe emptyList<Long>()
        search("_") shouldBe listOf(5L)
    }

    @Test
    fun `Empty search lists everything`() = runTest {
        search("") shouldBe listOf(1L, 2L, 3L, 4L, 5L)
    }

    private suspend fun search(query: String) = getHistory.subscribe(query).first().map { it.id }

    private fun entry(id: Long, title: String) = HistoryWithRelations(
        id = id,
        chapterId = id,
        mangaId = id,
        title = title,
        chapterNumber = 1.0,
        readAt = null,
        readDuration = 0,
        coverData = MangaCover(mangaId = id, sourceId = 1, isMangaFavorite = false, url = null, lastModified = null),
    )
}
