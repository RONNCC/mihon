package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `remote progress ahead of the chapter is not lowered but is stored locally`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = FakeQueue()
            val tracker = mockk<BaseTracker>()
            every { tracker.isLoggedIn } returns true
            coEvery { tracker.refresh(any()) } answers { firstArg<DbTrack>().also { it.last_chapter_read = 20.0 } }
            val upsertTrack = mockk<UpsertTrack>(relaxed = true)

            TrackChapter(getTracks(track(lastChapterRead = 5.0)), trackerManager(tracker), upsertTrack, store.store)
                .await(mockk(), 10L, 6.0)

            coVerify(exactly = 0) { tracker.update(any(), any()) }
            coVerify { upsertTrack.await(match { it.lastChapterRead == 20.0 }) }
        }

    @Test
    fun `a queued higher chapter survives a call for a chapter the local progress covers`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = FakeQueue(1L to 10.0)
            val tracker = mockk<BaseTracker>()
            every { tracker.isLoggedIn } returns true

            TrackChapter(getTracks(track(lastChapterRead = 8.0)), trackerManager(tracker), mockk(), store.store)
                .await(mockk(), 10L, 3.0, setupJobOnFailure = false)

            assertEquals(mapOf(1L to 10.0), store.items)
        }

    @Test
    fun `a queued chapter the local progress covers is dropped`() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeQueue(1L to 7.0)
        val tracker = mockk<BaseTracker>()
        every { tracker.isLoggedIn } returns true

        TrackChapter(getTracks(track(lastChapterRead = 8.0)), trackerManager(tracker), mockk(), store.store)
            .await(mockk(), 10L, 3.0, setupJobOnFailure = false)

        assertEquals(emptyMap<Long, Double>(), store.items)
    }

    @Test
    fun `a queued higher chapter survives a call for a chapter the remote progress covers`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = FakeQueue(1L to 10.0)
            val tracker = mockk<BaseTracker>()
            every { tracker.isLoggedIn } returns true
            coEvery { tracker.refresh(any()) } answers { firstArg<DbTrack>().also { it.last_chapter_read = 8.0 } }

            TrackChapter(
                getTracks(track(lastChapterRead = 2.0)),
                trackerManager(tracker),
                mockk(relaxed = true),
                store.store,
            )
                .await(mockk(), 10L, 5.0, setupJobOnFailure = false)

            assertEquals(mapOf(1L to 10.0), store.items)
        }

    private class FakeQueue(vararg initial: Pair<Long, Double>) {
        val items = mutableMapOf(*initial)
        val store = mockk<DelayedTrackingStore>().also { store ->
            every { store.remove(any()) } answers { items.remove(firstArg()) }
            every { store.removeIfCovered(any(), any()) } answers {
                if ((items[firstArg()] ?: Double.MAX_VALUE) <= secondArg<Double>()) items.remove(firstArg<Long>())
            }
            every { store.add(any(), any()) } answers { items[firstArg()] = secondArg() }
        }
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
