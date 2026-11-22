package eu.kanade.tachiyomi.data.backup

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Lets a notification through at most once per [interval], and always the one that [isLast].
 */
class NotificationThrottle(
    private val interval: Duration,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var lastShown: TimeMark? = null

    @Synchronized
    fun isDue(isLast: Boolean): Boolean {
        val shown = lastShown
        if (!isLast && shown != null && shown.elapsedNow() < interval) return false
        lastShown = timeSource.markNow()
        return true
    }
}
