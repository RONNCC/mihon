package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterRemoteUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Instant

class SyncChaptersWithSourceTest {

    private val manga = Manga.create().copy(id = 1, title = "Title")
    private val source = mockk<Source> { every { id } returns 1L }
    private val stored = Chapter.create().copy(
        id = 10,
        mangaId = 1,
        url = "/c1",
        name = "Chapter 1",
        scanlator = "Group",
        chapterNumber = 1.0,
        dateUpload = Instant.fromEpochMilliseconds(1_000),
        sourceOrder = 0,
    )

    private val chapterRepository = mockk<ChapterRepository> {
        coEvery { getChapterByMangaId(1) } returns listOf(stored)
        coEvery { updateFromRemote(any(), any(), any()) } returns emptyList()
    }
    private val downloadProvider = mockk<DownloadProvider> {
        every { isChapterDirNameChanged(any(), any()) } answers {
            firstArg<Chapter>().scanlator != secondArg<Chapter>().scanlator
        }
    }
    private val downloadManager = mockk<DownloadManager>(relaxed = true) {
        coEvery { isChapterDownloadedOnDisk(any(), any(), any(), any(), any()) } returns true
    }
    private val sync = SyncChaptersWithSource(
        downloadManager = downloadManager,
        downloadProvider = downloadProvider,
        chapterRepository = chapterRepository,
        shouldUpdateDbChapter = ShouldUpdateDbChapter(),
        updateManga = mockk(relaxed = true),
        getExcludedScanlators = mockk<GetExcludedScanlators> { coEvery { await(1) } returns emptySet() },
        libraryPreferences = LibraryPreferences(InMemoryPreferenceStore()),
    )

    @Test
    fun `chapter without an upload date does not change the stored one on every sync`() = runTest {
        sync.await(listOf(sourceChapter(scanlator = "Group", uploadedAt = 0)), manga, source)

        coVerify(exactly = 0) { chapterRepository.updateFromRemote(any(), any(), any()) }
    }

    @Test
    fun `dropped scanlator is stored as dropped and is what the download folder is renamed to`() = runTest {
        val updated = slot<List<ChapterRemoteUpdate>>()
        coEvery { chapterRepository.updateFromRemote(any(), any(), capture(updated)) } returns emptyList()
        val renamedTo = slot<Chapter>()
        coEvery { downloadManager.renameChapter(any(), any(), any(), capture(renamedTo)) } returns Unit

        sync.await(listOf(sourceChapter(scanlator = null, uploadedAt = 1_000)), manga, source)

        updated.captured.single().scanlator shouldBe null
        renamedTo.captured.scanlator shouldBe null
    }

    private fun sourceChapter(scanlator: String?, uploadedAt: Long) = SChapter.create().apply {
        url = "/c1"
        name = "Chapter 1"
        date_upload = uploadedAt
        chapter_number = 1f
        this.scanlator = scanlator
    }
}
