package eu.kanade.tachiyomi.data.backup

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class NotificationThrottleTest {

    private val time = TestTimeSource()
    private val throttle = NotificationThrottle(500.milliseconds, time)

    @Test
    fun `the first notification and one after the interval go through`() {
        throttle.isDue(isLast = false) shouldBe true

        time += 500.milliseconds

        throttle.isDue(isLast = false) shouldBe true
    }

    @Test
    fun `notifications within the interval are skipped`() {
        throttle.isDue(isLast = false) shouldBe true

        time += 499.milliseconds

        throttle.isDue(isLast = false) shouldBe false
        throttle.isDue(isLast = false) shouldBe false
    }

    @Test
    fun `a skipped notification does not extend the interval`() {
        throttle.isDue(isLast = false) shouldBe true
        time += 300.milliseconds
        throttle.isDue(isLast = false) shouldBe false
        time += 200.milliseconds

        throttle.isDue(isLast = false) shouldBe true
    }

    @Test
    fun `the last notification always goes through`() {
        throttle.isDue(isLast = false) shouldBe true

        throttle.isDue(isLast = true) shouldBe true
    }
}
