package mihon.data.extension.service

import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.domain.extension.model.ExtensionStore
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test

class ExtensionStoreServiceTest {

    private val network = mockk<NetworkHelper>()
    private val service = ExtensionStoreService(network, Json, ProtoBuf)

    @Test
    fun `A store index over plain HTTP is refused without being fetched`() = runTest {
        val result = service.fetch("http://example.com/index.min.json")

        result.exceptionOrNull()?.message shouldBe "Extension stores must use HTTPS: http://example.com/index.min.json"
        verify(exactly = 0) { network.client }
    }

    @Test
    fun `Extensions of a store saved with a plain HTTP index are refused without being fetched`() = runTest {
        val store = ExtensionStore(
            indexUrl = "http://example.com/index.pb",
            name = "Store",
            badgeLabel = "Store",
            signingKey = "key",
            contact = ExtensionStore.Contact(website = "https://example.com", discord = null),
            isLegacy = false,
            extensionListUrl = null,
        )

        val result = service.getExtensions(store)

        result.exceptionOrNull()?.message shouldBe "Extension stores must use HTTPS: http://example.com/index.pb"
        verify(exactly = 0) { network.client }
    }

    @Test
    fun `A store index redirected to plain HTTP is refused`() = runTest {
        // Answers as if the HTTPS index had redirected to HTTP
        every { network.client } returns OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request().newBuilder().url("http://example.com/index.min.json").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody())
                    .build()
            }
            .build()

        val result = service.fetch("https://example.com/index.min.json")

        result.exceptionOrNull()?.message shouldBe "Extension stores must use HTTPS: http://example.com/index.min.json"
    }
}
