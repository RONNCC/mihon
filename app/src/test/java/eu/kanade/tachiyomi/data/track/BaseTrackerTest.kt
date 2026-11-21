package eu.kanade.tachiyomi.data.track

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.Track as DomainTrack

class BaseTrackerTest {

    private lateinit var tracker: TestTracker

    @BeforeEach
    fun setUp() {
        tracker = TestTracker(
            dependencies = TrackerDependencies(
                context = mockk(relaxed = true),
                trackPreferences = mockk(relaxed = true),
                networkHelper = lazy { mockk() },
                addTracks = lazy { mockk() },
                upsertTrack = lazy { mockk(relaxed = true) },
            ),
        )
    }

    private fun createTrack(
        lastChapterRead: Double = 0.0,
        status: Long = TestTracker.PLAN_TO_READ,
        startDate: Long = 0L,
        finishDate: Long = 0L,
        totalChapters: Long = 0L,
    ): Track = Track.create(tracker.id).apply {
        title = "Test Manga"
        last_chapter_read = lastChapterRead
        this.status = status
        started_reading_date = startDate
        finished_reading_date = finishDate
        total_chapters = totalChapters
    }

    @Test
    fun `setRemoteLastChapterRead initializes start date when empty and transitioning to reading`() = runTest {
        val track = createTrack(
            lastChapterRead = 0.0,
            status = TestTracker.PLAN_TO_READ,
            startDate = 0L,
        )

        tracker.setRemoteLastChapterRead(track, 1)

        track.status shouldBe tracker.getReadingStatus()
        track.last_chapter_read shouldBe 1.0
        track.started_reading_date shouldBeGreaterThan 0L
    }

    @Test
    fun `setRemoteLastChapterRead preserves existing start date when transitioning to reading`() = runTest {
        val existingDate = 1_700_000_000_000L
        val track = createTrack(
            lastChapterRead = 0.0,
            status = TestTracker.PLAN_TO_READ,
            startDate = existingDate,
        )

        tracker.setRemoteLastChapterRead(track, 1)

        track.status shouldBe tracker.getReadingStatus()
        track.last_chapter_read shouldBe 1.0
        track.started_reading_date shouldBe existingDate
    }

    @Test
    fun `setRemoteLastChapterRead preserves status and start date when rereading`() = runTest {
        val existingDate = 1_700_000_000_000L
        val track = createTrack(
            lastChapterRead = 0.0,
            status = tracker.getRereadingStatus(),
            startDate = existingDate,
        )

        tracker.setRemoteLastChapterRead(track, 1)

        track.status shouldBe tracker.getRereadingStatus()
        track.last_chapter_read shouldBe 1.0
        track.started_reading_date shouldBe existingDate
    }

    @Test
    fun `setRemoteLastChapterRead does not modify start date on later chapters`() = runTest {
        val existingDate = 1_700_000_000_000L
        val track = createTrack(
            lastChapterRead = 1.0,
            status = tracker.getReadingStatus(),
            startDate = existingDate,
        )

        tracker.setRemoteLastChapterRead(track, 2)

        track.status shouldBe tracker.getReadingStatus()
        track.last_chapter_read shouldBe 2.0
        track.started_reading_date shouldBe existingDate
    }

    @Test
    fun `setRemoteLastChapterRead handles completion and sets finish date`() = runTest {
        val existingStartDate = 1_700_000_000_000L
        val track = createTrack(
            lastChapterRead = 5.0,
            totalChapters = 10L,
            status = tracker.getReadingStatus(),
            startDate = existingStartDate,
            finishDate = 0L,
        )

        tracker.setRemoteLastChapterRead(track, 10)

        track.status shouldBe tracker.getCompletionStatus()
        track.last_chapter_read shouldBe 10.0
        track.started_reading_date shouldBe existingStartDate
        track.finished_reading_date shouldBeGreaterThan 0L
    }

    @Test
    fun `setRemoteLastChapterRead sets both start and finish dates for single chapter read to completion`() = runTest {
        val track = createTrack(
            lastChapterRead = 0.0,
            totalChapters = 1L,
            status = TestTracker.PLAN_TO_READ,
            startDate = 0L,
            finishDate = 0L,
        )

        tracker.setRemoteLastChapterRead(track, 1)

        track.status shouldBe tracker.getCompletionStatus()
        track.last_chapter_read shouldBe 1.0
        track.started_reading_date shouldBeGreaterThan 0L
        track.finished_reading_date shouldBeGreaterThan 0L
    }

    private class TestTracker(
        id: Long = 1L,
        name: String = "TestTracker",
        dependencies: TrackerDependencies,
    ) : BaseTracker(id, name, dependencies) {
        override fun getLogo(): Int = 0
        override fun getStatusList(): List<Long> = listOf(READING, PLAN_TO_READ, COMPLETED, REREADING)
        override fun getStatus(status: Long): StringResource? = null
        override fun getReadingStatus(): Long = READING
        override fun getCompletionStatus(): Long = COMPLETED
        override fun getRereadingStatus(): Long = REREADING
        override fun getScoreList(): List<String> = emptyList()
        override fun displayScore(track: DomainTrack): String = ""
        override suspend fun update(track: Track, didReadChapter: Boolean): Track = track
        override suspend fun bind(track: Track, hasReadChapters: Boolean): Track = track
        override suspend fun search(query: String): List<TrackSearch> = emptyList()
        override suspend fun refresh(track: Track): Track = track
        override suspend fun login(username: String, password: String) {}
        override suspend fun updateUserConfig() {}

        companion object {
            const val READING = 1L
            const val COMPLETED = 2L
            const val REREADING = 3L
            const val PLAN_TO_READ = 4L
        }
    }
}
