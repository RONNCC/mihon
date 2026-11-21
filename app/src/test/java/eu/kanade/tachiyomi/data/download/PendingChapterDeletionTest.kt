package eu.kanade.tachiyomi.data.download

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager

class PendingChapterDeletionTest {

    @Test
    fun `deletes the pending chapters of an uninstalled source`() = runTest {
        val manga = Manga.create().copy(id = 1, source = 2, title = "Uninstalled")
        val chapters = listOf(Chapter.create().copy(id = 3, mangaId = manga.id, read = true))
        val stub = StubSource(id = manga.source, lang = "en", name = "Gone")
        val sourceManager = mockk<SourceManager>()
        val getCategories = mockk<GetCategories>()
        val pendingDeleter = mockk<DownloadPendingDeleter>()
        val provider = mockk<DownloadProvider>()
        coEvery { sourceManager.getOrStub(manga.source) } returns stub
        coEvery { getCategories.await(manga.id) } returns emptyList()
        coEvery { provider.findChapterDirs(chapters, manga, stub) } returns (null to emptyList())
        every { pendingDeleter.getPendingChapters() } returns mapOf(manga to chapters)
        val manager = DownloadManager(
            mockk(), provider, mockk(relaxed = true), getCategories, mockk(), mockk(), sourceManager,
            DownloadPreferences(InMemoryPreferenceStore()), mockk(relaxed = true), pendingDeleter,
        )

        manager.deletePendingChapters()

        coVerify { provider.findChapterDirs(chapters, manga, stub) }
    }
}
