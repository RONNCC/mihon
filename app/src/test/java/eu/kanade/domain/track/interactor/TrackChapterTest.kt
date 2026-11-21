package eu.kanade.domain.track.interactor

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
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.UpsertTrack
import tachiyomi.domain.track.model.Track

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
}
