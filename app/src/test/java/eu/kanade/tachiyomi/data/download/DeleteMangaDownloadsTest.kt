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
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

class DeleteMangaDownloadsTest {

    private val manga = Manga.create().copy(id = 1, source = 2, title = "Shared title")
    private val sameTitleOtherSource = Manga.create().copy(id = 9, source = 22, title = "shared title")
    private val source = mockk<Source>()
    private val otherSource = mockk<Source>()
    private val chapter = Chapter.create().copy(id = 3, mangaId = manga.id, read = true, bookmark = true)
    private val chapterDir = mockk<UniFile>()
    private val mangaDir = mockk<UniFile>()
    private val provider = mockk<DownloadProvider>()
    private val cache = mockk<DownloadCache>(relaxed = true)
    private val downloader = mockk<Downloader>(relaxed = true)
    private val getManga = mockk<GetManga>()
    private val sourceManager = mockk<SourceManager>()
    private val manager = DownloadManager(
        mockk(), provider, cache, mockk(), getManga, mockk(), sourceManager,
        DownloadPreferences(InMemoryPreferenceStore()), downloader, mockk(),
    )

    init {
        coEvery { sourceManager.getOrStub(22) } returns otherSource
        every { provider.getSourceDirName(source) } returns "Source (EN)"
        every { provider.getMangaDirName(any()) } returns "Shared title"
        coEvery { provider.findChapterDirs(listOf(chapter), manga, source) } returns (mangaDir to listOf(chapterDir))
        coEvery { provider.findMangaDir(manga.title, source) } returns mangaDir
        coEvery { provider.findSourceDir(source) } returns null
        every { chapterDir.delete() } returns true
        every { mangaDir.delete() } returns true
    }

    @Test
    fun `keeps the directory shared with another entry of the same title and source name`() = runTest {
        every { provider.getSourceDirName(otherSource) } returns "Source (EN)"
        coEvery { getManga.awaitByTitle(manga.title) } returns listOf(manga, sameTitleOtherSource)
        every { mangaDir.listFiles() } returns arrayOf(mockk<UniFile>())

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify(exactly = 0) { mangaDir.delete() }
        coVerify(exactly = 0) { cache.removeManga(any()) }
        coVerify { downloader.removeFromQueue(manga) }
    }

    @Test
    fun `deletes the whole directory of an entry nothing else shares`() = runTest {
        every { provider.getSourceDirName(otherSource) } returns "Other (EN)"
        coEvery { getManga.awaitByTitle(manga.title) } returns listOf(manga, sameTitleOtherSource)

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        // Leftovers of removed chapters and interrupted downloads go with the directory
        verify(exactly = 0) { chapterDir.delete() }
        verify { mangaDir.delete() }
        coVerify { cache.removeManga(manga) }
        coVerify { downloader.removeFromQueue(manga) }
    }

    @Test
    fun `deletes only the chapters when the entries of the title can't be read`() = runTest {
        coEvery { getManga.awaitByTitle(manga.title) } returns null
        every { mangaDir.listFiles() } returns arrayOf(mockk<UniFile>())

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify(exactly = 0) { mangaDir.delete() }
    }

    @Test
    fun `deletes the directory once its last chapter is deleted when shared`() = runTest {
        every { provider.getSourceDirName(otherSource) } returns "Source (EN)"
        coEvery { getManga.awaitByTitle(manga.title) } returns listOf(manga, sameTitleOtherSource)
        every { mangaDir.listFiles() } returns emptyArray()

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify { mangaDir.delete() }
        coVerify { cache.removeManga(manga) }
    }
}
