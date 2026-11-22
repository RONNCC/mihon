package eu.kanade.domain.track.interactor

import eu.kanade.tachiyomi.data.track.Tracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

class AddTracksTest {

    @Test
    fun `Unrecognized chapters do not hide the local progress from the tracker`() = runTest {
        val chapters = listOf(
            chapter(number = -1.0, read = false),
            chapter(number = 1.0, read = true),
            chapter(number = 2.0, read = true),
            chapter(number = 3.0, read = false),
        )
        val tracker = mockk<Tracker>(relaxed = true)
        val addTracks = AddTracks(
            upsertTrack = mockk(relaxed = true),
            syncChapterProgressWithTrack = mockk(relaxed = true),
            getChaptersByMangaId = mockk { coEvery { await(10L) } returns chapters },
            trackerManager = mockk(),
            getHistory = mockk { coEvery { await(10L) } returns emptyList() },
        )

        val track = DbTrack.create(2L).apply {
            manga_id = 10L
            title = "Title"
        }

        addTracks.bind(tracker, track, 10L)

        coVerify { tracker.setRemoteLastChapterRead(any(), 2) }
    }

    private fun chapter(number: Double, read: Boolean) = Chapter.create().copy(chapterNumber = number, read = read)
}
