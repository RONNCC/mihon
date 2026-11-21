package eu.kanade.tachiyomi.data.download

import android.content.Context
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DownloaderQueueTest {

    private val manga = Manga.create().copy(id = 1, source = 2, title = "Queue")
    private val source = mockk<HttpSource>(relaxed = true)
    private val store = mockk<DownloadStore>(relaxed = true)
    private val provider = mockk<DownloadProvider>()

    @BeforeEach
    fun setMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { store.restore() } returns emptyList()
        // A failing download directory ends every download at once, so nothing touches the network or the disk
        coEvery { provider.getMangaDir(any(), any()) } returns Result.failure(IOException("no storage"))
        coEvery { provider.findDownloadedChapterIds(any(), any(), any()) } returns emptySet()
    }

    @AfterEach
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private fun downloader(scope: CoroutineScope): Downloader {
        val sourceManager = mockk<SourceManager>()
        coEvery { sourceManager.get(manga.source) } returns source
        return Downloader(
            scope, mockk<Context>(), provider, mockk(relaxed = true), sourceManager, mockk(relaxed = true),
            DownloadPreferences(InMemoryPreferenceStore()), mockk(), mockk(), mockk(), store, mockk(relaxed = true),
        )
    }

    private fun chapter(id: Long) = Chapter.create().copy(id = id, mangaId = manga.id, sourceOrder = id)

    @Test
    fun `start waits for the queue to be restored`() = runTest {
        val restore = CompletableDeferred<List<Download>>()
        coEvery { store.restore() } coAnswers { restore.await() }
        val downloader = downloader(backgroundScope)

        val started = async(start = CoroutineStart.UNDISPATCHED) { downloader.start() }
        assertFalse(started.isCompleted)
        restore.complete(listOf(Download(source, manga, chapter(10))))

        assertTrue(started.await())
    }

    @Test
    fun `queueing starts a stopped downloader that still holds chapters`() = runTest {
        val downloader = downloader(backgroundScope)
        assertFalse(downloader.queueChapters(manga, listOf(chapter(10)), autoStart = false))

        assertTrue(downloader.queueChapters(manga, listOf(chapter(11)), autoStart = true))
        assertEquals(listOf(10L, 11L), downloader.queueState.value.map { it.chapter.id })
    }

    @Test
    fun `queueing doesn't start a downloader the user paused`() = runTest {
        val downloader = downloader(backgroundScope)
        downloader.queueChapters(manga, listOf(chapter(10)), autoStart = false)
        downloader.pause()

        assertFalse(downloader.queueChapters(manga, listOf(chapter(11)), autoStart = true))
        assertEquals(2, downloader.queueState.value.size)
    }

    @Test
    fun `removing the only manga in the queue stops the downloader`() = runTest {
        // Keeps the download running until it is canceled
        val storage = CompletableDeferred<Nothing>()
        coEvery { provider.getMangaDir(any(), any()) } coAnswers { storage.await() }
        val downloader = downloader(backgroundScope)
        downloader.queueChapters(manga, listOf(chapter(10)), autoStart = false)
        assertTrue(downloader.start())
        assertTrue(downloader.isRunning)

        downloader.removeFromQueue(manga)

        assertFalse(downloader.isRunning)
        assertTrue(downloader.queueState.value.isEmpty())
    }
}
