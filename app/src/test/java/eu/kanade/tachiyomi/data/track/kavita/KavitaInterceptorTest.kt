package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.data.track.TrackerDependencies
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

class KavitaInterceptorTest {

    private val sourceManager = mockk<SourceManager>().also { coEvery { it.get(any()) } returns null }

    private val kavita = Kavita(
        dependencies = TrackerDependencies(
            context = mockk(relaxed = true),
            trackPreferences = mockk(relaxed = true),
            networkHelper = lazy { mockk() },
            addTracks = lazy { mockk() },
            upsertTrack = lazy { mockk(relaxed = true) },
        ),
        json = Json,
        sourceManager = lazy { sourceManager },
    )

    @Test
    fun `missing extension is skipped instead of crashing`() {
        kavita.loadOAuth()

        kavita.authentications.shouldBeNull()
    }

    @Test
    fun `request without a configured source fails with an IOException`() {
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns Request.Builder().url("https://kavita.example/api/Series/1").build()
        }

        assertThrows(IOException::class.java) { KavitaInterceptor(kavita).intercept(chain) }
    }
}
