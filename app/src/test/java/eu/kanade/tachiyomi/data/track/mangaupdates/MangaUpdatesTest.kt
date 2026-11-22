package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerDependencies
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MangaUpdatesTest {

    private val sentLists = mutableListOf<Int>()

    private lateinit var tracker: MangaUpdates

    @BeforeEach
    fun setUp() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.encodedPath == "/v1/lists/series/update") {
                    val body = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
                    val item = Json.parseToJsonElement(body).jsonArray.first().jsonObject
                    sentLists += item["list_id"]!!.jsonPrimitive.int
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()
        val networkHelper = mockk<NetworkHelper> { every { this@mockk.client } returns client }
        tracker = MangaUpdates(
            dependencies = TrackerDependencies(
                context = mockk(relaxed = true),
                trackPreferences = mockk(relaxed = true) {
                    every { trackPassword(any()).get() } returns "session"
                },
                networkHelper = lazy { networkHelper },
                addTracks = lazy { mockk() },
                upsertTrack = lazy { mockk(relaxed = true) },
            ),
            json = Json,
        )
    }

    private suspend fun updateAfterRead(status: Long): Track {
        val track = Track.create(tracker.id).apply {
            remote_id = 1L
            this.status = status
            last_chapter_read = 5.0
            score = -1.0
        }
        return tracker.update(track, didReadChapter = true)
    }

    @Test
    fun `reading a chapter moves a built-in list to reading`() = runTest {
        updateAfterRead(MangaUpdates.WISH_LIST).status shouldBe MangaUpdates.READING_LIST

        sentLists shouldBe listOf(MangaUpdates.READING_LIST.toInt())
    }

    @Test
    fun `reading a chapter keeps a completed series completed`() = runTest {
        updateAfterRead(MangaUpdates.COMPLETE_LIST).status shouldBe MangaUpdates.COMPLETE_LIST
    }

    @Test
    fun `reading a chapter keeps a series in its custom list`() = runTest {
        updateAfterRead(CUSTOM_LIST).status shouldBe CUSTOM_LIST

        sentLists shouldBe listOf(CUSTOM_LIST.toInt())
    }

    private companion object {
        const val CUSTOM_LIST = 7L
    }
}
