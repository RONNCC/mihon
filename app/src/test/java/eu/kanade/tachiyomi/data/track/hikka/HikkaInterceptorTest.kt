package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.hikka.dto.HKOAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException

class HikkaInterceptorTest {

    private val expired = HKOAuth(accessToken = "token", expiration = 0L, created = 0L)

    private val hikka = mockk<Hikka>(relaxed = true).also { every { it.loadOAuth() } returns expired }

    private fun chainRespondingWith(code: Int): Interceptor.Chain {
        val request = Request.Builder().url("https://api.hikka.io/user/me").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .body("".toResponseBody())
            .build()
        return chain
    }

    @Test
    fun `a transient server error keeps the user logged in`() {
        assertThrows(IOException::class.java) { HikkaInterceptor(hikka, Json).intercept(chainRespondingWith(503)) }

        verify(exactly = 0) { hikka.logout() }
    }

    @Test
    fun `a rejected token logs the user out`() {
        assertThrows(Exception::class.java) { HikkaInterceptor(hikka, Json).intercept(chainRespondingWith(401)) }

        verify(exactly = 1) { hikka.logout() }
    }
}
