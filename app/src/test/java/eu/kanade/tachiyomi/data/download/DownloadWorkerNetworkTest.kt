package eu.kanade.tachiyomi.data.download

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DownloadWorkerNetworkTest {

    private val events = mutableListOf<String>()

    private suspend fun watch(vararg samples: NetworkIssue?): NetworkIssue? {
        val remaining = ArrayDeque(samples.toList())
        return watchNetwork(
            isRunning = { remaining.isNotEmpty() },
            currentIssue = { remaining.removeFirst() },
            suspendDownloads = { events += "suspend" },
            resumeDownloads = { events += "resume" },
        )
    }

    @Test
    fun `a connection back within the grace period resumes the downloads`() = runTest {
        val offline = List(NetworkIssue.Offline.graceSeconds) { NetworkIssue.Offline }

        assertNull(watch(*offline.toTypedArray(), null, *offline.toTypedArray(), null))
        assertEquals(listOf("suspend", "resume", "suspend", "resume"), events)
    }

    @Test
    fun `a connection lost past the grace period is reported with the downloads suspended`() = runTest {
        val offline = Array<NetworkIssue?>(NetworkIssue.Offline.graceSeconds + 1) { NetworkIssue.Offline }

        assertEquals(NetworkIssue.Offline, watch(*offline, null))
        assertEquals(listOf("suspend"), events)
        assertEquals((NetworkIssue.Offline.graceSeconds + 1) * 1000L, currentTime)
    }

    @Test
    fun `leaving Wi-Fi is reported at once`() = runTest {
        assertEquals(NetworkIssue.NotWifi, watch(NetworkIssue.NotWifi, null))
        assertEquals(listOf("suspend"), events)
        assertEquals(1000L, currentTime)
    }

    @Test
    fun `a connection that stays fine leaves the downloads alone`() = runTest {
        assertNull(watch(null, null, null))
        assertEquals(emptyList<String>(), events)
    }
}
