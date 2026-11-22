package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaTitle
import tachiyomi.domain.source.service.SourceManager

class DeleteMangaDownloadsTest {

    private val manga = Manga.create().copy(id = 1, source = 2, title = "Re:Zero")
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
        every { provider.getMangaDirName(any()) } answers { DiskUtil.buildValidFilename(firstArg()) }
        coEvery { provider.findChapterDirs(listOf(chapter), manga, source) } returns (mangaDir to listOf(chapterDir))
        coEvery { provider.findMangaDir(manga.title, source) } returns mangaDir
        coEvery { provider.findSourceDir(source) } returns null
        every { chapterDir.delete() } returns true
        every { mangaDir.delete() } returns true
    }

    @ParameterizedTest
    @ValueSource(strings = ["Re:Zero", "re:zero", "Re?Zero", "Re_Zero", "RE?ZERO"])
    fun `keeps the directory shared with another entry whose title gives the same folder`(title: String) = runTest {
        every { provider.getSourceDirName(otherSource) } returns "SOURCE (EN)"
        coEvery { getManga.awaitTitles() } returns listOf(MangaTitle(1, 2, manga.title), MangaTitle(9, 22, title))
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
        coEvery { getManga.awaitTitles() } returns
            listOf(MangaTitle(1, 2, manga.title), MangaTitle(9, 22, manga.title), MangaTitle(10, 2, "Other title"))

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        // Leftovers of removed chapters and interrupted downloads go with the directory
        verify(exactly = 0) { chapterDir.delete() }
        verify { mangaDir.delete() }
        coVerify { cache.removeManga(manga) }
        coVerify { downloader.removeFromQueue(manga) }
    }

    @Test
    fun `deletes only the chapters when the entries of the title can't be read`() = runTest {
        coEvery { getManga.awaitTitles() } returns null
        every { mangaDir.listFiles() } returns arrayOf(mockk<UniFile>())

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify(exactly = 0) { mangaDir.delete() }
    }

    @Test
    fun `deletes the directory once its last chapter is deleted when shared`() = runTest {
        every { provider.getSourceDirName(otherSource) } returns "Source (EN)"
        coEvery { getManga.awaitTitles() } returns listOf(MangaTitle(1, 2, manga.title), MangaTitle(9, 22, manga.title))
        every { mangaDir.listFiles() } returns emptyArray()

        manager.deleteMangaDownloads(listOf(chapter), manga, source)

        verify { chapterDir.delete() }
        verify { mangaDir.delete() }
        coVerify { cache.removeManga(manga) }
    }
}
