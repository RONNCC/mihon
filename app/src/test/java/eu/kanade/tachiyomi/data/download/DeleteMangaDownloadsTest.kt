package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga

class DeleteMangaDownloadsTest {

    private val manga = Manga.create().copy(id = 1, source = 2, title = "Shared title")
    private val source = mockk<Source>()
    private val chapter = Chapter.create().copy(id = 3, mangaId = manga.id, read = true, bookmark = true)
    private val chapterDir = mockk<UniFile>()
    private val mangaDir = mockk<UniFile>()
    private val provider = mockk<DownloadProvider>()
    private val cache = mockk<DownloadCache>(relaxed = true)
    private val downloader = mockk<Downloader>(relaxed = true)
    private val manager = DownloadManager(
        mockk(), provider, cache, mockk(), mockk(), mockk(), mockk(),
        DownloadPreferences(InMemoryPreferenceStore()), downloader, mockk(),
    )

    init {
        coEvery { provider.findChapterDirs(listOf(chapter), manga, source) } returns (mangaDir to listOf(chapterDir))
        every { chapterDir.delete() } returns true
    }

    @Test
    fun `keeps the directory shared with another entry of the same title`() = runTest {
        every { mangaDir.listFiles() } returns arrayOf(mockk<UniFile>())

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify(exactly = 0) { mangaDir.delete() }
        coVerify(exactly = 0) { cache.removeManga(any()) }
        coVerify { downloader.removeFromQueue(manga) }
    }

    @Test
    fun `deletes the directory once its last chapter is deleted regardless of the removal settings`() = runTest {
        every { mangaDir.listFiles() } returns emptyArray()
        every { mangaDir.delete() } returns true
        coEvery { provider.findMangaDir(manga.title, source) } returns mangaDir
        coEvery { provider.findSourceDir(source) } returns null

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify { mangaDir.delete() }
        coVerify { cache.removeManga(manga) }
    }
}
