package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.UpsertTrack
import tachiyomi.domain.track.model.Track
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

class TrackChapterTest {

    @Test
    fun `concurrent updates for the same chapter reach the tracker once`() = runTest(UnconfinedTestDispatcher()) {
        var stored = Track(
            id = 1L,
            mangaId = 10L,
            trackerId = 2L,
            remoteId = 3L,
            libraryId = null,
            title = "title",
            lastChapterRead = 4.0,
            totalChapters = 0L,
            status = 1L,
            score = 0.0,
            remoteUrl = "",
            startDate = null,
            finishDate = null,
            private = false,
        )
        val getTracks = mockk<GetTracks>()
        coEvery { getTracks.await(10L) } answers { listOf(stored) }
        val upsertTrack = mockk<UpsertTrack>()
        coEvery { upsertTrack.await(any()) } answers { stored = firstArg() }

        val updateStarted = CompletableDeferred<Unit>()
        val finishUpdate = CompletableDeferred<Unit>()
        val tracker = mockk<BaseTracker>()
        every { tracker.isLoggedIn } returns true
        coEvery { tracker.refresh(any()) } answers { firstArg() }
        coEvery { tracker.update(any(), true) } coAnswers {
            updateStarted.complete(Unit)
            finishUpdate.await()
            firstArg()
        }
        val trackerManager = mockk<TrackerManager>()
        every { trackerManager.get(2L) } returns tracker

        val trackChapter = TrackChapter(getTracks, trackerManager, upsertTrack, mockk(relaxed = true))

        val first = launch { trackChapter.await(mockk(), 10L, 5.0) }
        updateStarted.await()
        val second = launch { trackChapter.await(mockk(), 10L, 5.0) }
        finishUpdate.complete(Unit)
        first.join()
        second.join()

        coVerify(exactly = 1) { tracker.update(any(), true) }
    }

    @Test
    fun `remote progress ahead of the chapter is not lowered`() = runTest(UnconfinedTestDispatcher()) {
        val store = mockk<DelayedTrackingStore>(relaxed = true)
        val tracker = mockk<BaseTracker>()
        every { tracker.isLoggedIn } returns true
        coEvery { tracker.refresh(any()) } answers { firstArg<DbTrack>().also { it.last_chapter_read = 20.0 } }

        TrackChapter(getTracks(track(lastChapterRead = 5.0)), trackerManager(tracker), mockk(), store)
            .await(mockk(), 10L, 6.0)

        coVerify(exactly = 0) { tracker.update(any(), any()) }
        verify { store.remove(1L) }
    }

    @Test
    fun `stale delayed entry is dropped when the local progress is already ahead`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val store = mockk<DelayedTrackingStore>(relaxed = true)
        val tracker = mockk<BaseTracker>()
        every { tracker.isLoggedIn } returns true

        TrackChapter(getTracks(track(lastChapterRead = 10.0)), trackerManager(tracker), mockk(), store)
            .await(mockk(), 10L, 7.0, setupJobOnFailure = false)

        verify { store.remove(1L) }
        coVerify(exactly = 0) { tracker.refresh(any()) }
    }

    private fun track(lastChapterRead: Double) = Track(
        id = 1L,
        mangaId = 10L,
        trackerId = 2L,
        remoteId = 3L,
        libraryId = null,
        title = "title",
        lastChapterRead = lastChapterRead,
        totalChapters = 0L,
        status = 1L,
        score = 0.0,
        remoteUrl = "",
        startDate = null,
        finishDate = null,
        private = false,
    )

    private fun getTracks(track: Track) = mockk<GetTracks>().also { coEvery { it.await(10L) } returns listOf(track) }

    private fun trackerManager(tracker: BaseTracker) = mockk<TrackerManager>().also {
        every { it.get(2L) } returns tracker
    }
}
