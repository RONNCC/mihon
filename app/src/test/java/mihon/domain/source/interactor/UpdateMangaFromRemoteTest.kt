package mihon.domain.source.interactor

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class UpdateMangaFromRemoteTest {

    private val manga = Manga.create().copy(id = 1, source = 10, thumbnailUrl = "https://old/cover")
    private val source = mockk<Source>()
    private val mangaRepository = mockk<MangaRepository>()
    private val coverCache = mockk<CoverCache>(relaxed = true)

    private val updateMangaFromRemote = UpdateMangaFromRemote(
        sourceManager = mockk(),
        chapterRepository = mockk { coEvery { getChapterByMangaId(manga.id) } returns emptyList() },
        mangaRepository = mangaRepository,
        syncChaptersWithSource = mockk { coEvery { await(any(), any(), any(), any(), any()) } returns emptyList() },
        coverCache = coverCache,
        libraryPreferences = mockk(relaxed = true),
        downloadManager = mockk(relaxed = true),
    )

    private fun givenRemote(updated: Boolean) {
        val remote = SManga.create().apply {
            title = manga.title
            thumbnail_url = "https://new/cover"
        }
        coEvery { source.getMangaUpdate(any(), any(), any(), any()) } returns SMangaUpdate(remote, emptyList())
        coEvery { mangaRepository.updateRemote(any()) } returns updated
        coEvery { mangaRepository.getMangaById(manga.id) } returns manga
    }

    @Test
    fun `the old cover is kept when the update is not stored`() = runTest {
        givenRemote(updated = false)

        updateMangaFromRemote(source, manga, fetchDetails = true)

        coVerify { mangaRepository.updateRemote(any()) }
        verify(exactly = 0) { coverCache.deleteFromCache(any(), any()) }
    }

    @Test
    fun `the old cover is dropped once the update is stored`() = runTest {
        givenRemote(updated = true)

        updateMangaFromRemote(source, manga, fetchDetails = true)

        verify(exactly = 1) { coverCache.deleteFromCache(manga, false) }
    }
}
