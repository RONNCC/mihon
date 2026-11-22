package mihon.data.extension.service

import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test

class ExtensionStoreServiceTest {

    private val network = mockk<NetworkHelper>()
    private val service = ExtensionStoreService(network, Json, ProtoBuf)

    @Test
    fun `A store index over plain HTTP is fetched`() = runTest {
        val requested = mutableListOf<String>()
        every { network.client } returns OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                requested += url
                val body = if (url.endsWith("/repo.json")) {
                    """
                    {"index_v2":null,"meta":{"name":"Store","shortName":"S","website":"http://example.com","signingKeyFingerprint":"key"}}
                    """.trimIndent()
                } else {
                    "[]"
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody())
                    .build()
            }
            .build()

        val result = service.fetch("http://example.com/index.min.json")

        result.getOrThrow().signingKey shouldBe "key"
        requested shouldBe listOf("http://example.com/index.min.json", "http://example.com/repo.json")
    }
}
