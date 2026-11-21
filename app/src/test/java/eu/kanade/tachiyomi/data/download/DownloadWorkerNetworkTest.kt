package eu.kanade.tachiyomi.data.download

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DownloadWorkerNetworkTest {

    private suspend fun awaitIssue(vararg samples: NetworkIssue?): NetworkIssue? {
        val remaining = ArrayDeque(samples.toList())
        return awaitNetworkIssue({ remaining.isNotEmpty() }, { remaining.removeFirst() })
    }

    @Test
    fun `a connection lost for less than the grace period doesn't stop the downloads`() = runTest {
        val offline = List(NetworkIssue.Offline.graceSeconds) { NetworkIssue.Offline }

        assertNull(awaitIssue(*offline.toTypedArray(), null, *offline.toTypedArray()))
    }

    @Test
    fun `a connection lost past the grace period stops the downloads`() = runTest {
        val offline = Array<NetworkIssue?>(NetworkIssue.Offline.graceSeconds + 1) { NetworkIssue.Offline }

        assertEquals(NetworkIssue.Offline, awaitIssue(*offline, null))
        assertEquals((NetworkIssue.Offline.graceSeconds + 1) * 1000L, currentTime)
    }

    @Test
    fun `leaving Wi-Fi stops the downloads at once`() = runTest {
        assertEquals(NetworkIssue.NotWifi, awaitIssue(NetworkIssue.NotWifi, null))
        assertEquals(1000L, currentTime)
    }
}
