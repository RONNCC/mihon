package eu.kanade.tachiyomi.ui.manga.track

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class TrackDateSelectableTest {

    private val helsinki = TimeZone.of("Europe/Helsinki")
    private val losAngeles = TimeZone.of("America/Los_Angeles")

    // Stored dates are local midnights, the picker reports the same day as UTC midnight
    private fun localMidnight(date: String, zone: TimeZone) = LocalDate.parse(date).atStartOfDayIn(zone)

    private fun pickerMillis(date: String) = LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    private val now = Instant.parse("2026-06-10T12:00:00Z")

    @Test
    fun `start date can equal finish date in a positive time zone`() {
        val finish = localMidnight("2026-06-05", helsinki)

        assertTrue(isSelectableTrackDate(pickerMillis("2026-06-05"), true, null, finish, now, helsinki))
        assertFalse(isSelectableTrackDate(pickerMillis("2026-06-06"), true, null, finish, now, helsinki))
    }

    @Test
    fun `finish date cannot precede start date in a positive time zone`() {
        val start = localMidnight("2026-06-05", helsinki)

        assertFalse(isSelectableTrackDate(pickerMillis("2026-06-04"), false, start, null, now, helsinki))
        assertTrue(isSelectableTrackDate(pickerMillis("2026-06-05"), false, start, null, now, helsinki))
    }

    @Test
    fun `start date can equal finish date in a negative time zone`() {
        val finish = localMidnight("2026-06-05", losAngeles)

        assertTrue(isSelectableTrackDate(pickerMillis("2026-06-05"), true, null, finish, now, losAngeles))
        assertFalse(isSelectableTrackDate(pickerMillis("2026-06-06"), true, null, finish, now, losAngeles))
    }

    @Test
    fun `future days are judged by the local date`() {
        // 2026-06-10 22:00 UTC is already June 11 in Helsinki and still June 10 in Los Angeles
        val lateNow = Instant.parse("2026-06-10T22:00:00Z")

        assertTrue(isSelectableTrackDate(pickerMillis("2026-06-11"), true, null, null, lateNow, helsinki))
        assertFalse(isSelectableTrackDate(pickerMillis("2026-06-11"), true, null, null, lateNow, losAngeles))
    }

    @Test
    fun `years are compared in the local time zone`() {
        // Local midnight of 2026-01-01 is still 2025 in UTC
        val start = localMidnight("2026-01-01", helsinki)

        assertFalse(isSelectableTrackYear(2025, false, start, null, now, helsinki))
        assertTrue(isSelectableTrackYear(2026, false, start, null, now, helsinki))
    }
}
