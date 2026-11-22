package mihon.domain.migration.usecases

import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mihon.domain.migration.models.MigrationFlag
import mihon.domain.source.interactor.UpdateMangaFromRemote
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import java.io.IOException

class MigrateMangaUseCaseTest {

    private val current = Manga.create().copy(id = 1, source = 10, title = "Old")
    private val target = Manga.create().copy(id = 2, source = 20, title = "New")
    private val currentChapters = listOf(Chapter.create().copy(id = 5, mangaId = current.id))
    private val currentSource = mockk<Source>()
    private val targetSource = mockk<Source>()

    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private val updateManga = mockk<UpdateManga>()
    private val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()

    private fun TestScope.useCase(): MigrateMangaUseCase {
        val preferences = SourcePreferences(InMemoryPreferenceStore())
        preferences.migrationFlags.set(setOf(MigrationFlag.REMOVE_DOWNLOAD))
        val sourceManager = mockk<SourceManager> {
            coEvery { get(current.source) } returns currentSource
            coEvery { get(target.source) } returns targetSource
        }
        val getChapters = mockk<GetChaptersByMangaId> {
            coEvery { await(current.id) } returns currentChapters
        }
        val getTracks = mockk<GetTracks> { coEvery { await(current.id) } returns emptyList() }
        return MigrateMangaUseCase(
            scope = this,
            sourcePreferences = preferences,
            trackerManager = mockk(relaxed = true),
            sourceManager = sourceManager,
            downloadManager = downloadManager,
            updateManga = updateManga,
            getChaptersByMangaId = getChapters,
            updateChapter = mockk(),
            getCategories = mockk(),
            setMangaCategories = mockk(),
            getTracks = getTracks,
            upsertTrack = mockk(),
            coverCache = mockk(),
            updateMangaFromRemote = updateMangaFromRemote,
        )
    }

    @Test
    fun `keeps the downloads when the fetch of the target fails`() = runTest {
        coEvery { updateMangaFromRemote(target, fetchChapters = true) } returns Result.failure(IOException())

        useCase().invoke(current, target, replace = true) shouldBe false
        advanceUntilIdle()

        coVerify(exactly = 0) { downloadManager.deleteMangaDownloads(any(), any(), any()) }
    }

    @Test
    fun `keeps the downloads when the database update fails`() = runTest {
        coEvery { updateMangaFromRemote(target, fetchChapters = true) } returns Result.success(mockk())
        coEvery { updateManga.awaitAll(any()) } returns false

        useCase().invoke(current, target, replace = true) shouldBe false
        advanceUntilIdle()

        coVerify(exactly = 0) { downloadManager.deleteMangaDownloads(any(), any(), any()) }
    }

    @Test
    fun `deletes the downloads of the current entry once migrated`() = runTest {
        coEvery { updateMangaFromRemote(target, fetchChapters = true) } returns Result.success(mockk())
        coEvery { updateManga.awaitAll(any()) } returns true

        useCase().invoke(current, target, replace = true) shouldBe true
        advanceUntilIdle()

        coVerify { downloadManager.deleteMangaDownloads(currentChapters, current, currentSource) }
    }
}
