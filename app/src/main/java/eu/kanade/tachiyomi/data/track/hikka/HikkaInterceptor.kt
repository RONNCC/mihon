package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.hikka.dto.HKAuthTokenInfo
import eu.kanade.tachiyomi.data.track.hikka.dto.HKOAuth
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HikkaInterceptor(
    private val hikka: Hikka,
    private val json: Json,
) : Interceptor {

    @Volatile
    private var oauth: HKOAuth? = hikka.loadOAuth()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        var currAuth = oauth ?: throw Exception("Hikka: You are not authorized")

        if (currAuth.isExpired()) {
            currAuth = refreshToken(chain, currAuth)
        }

        val authRequest = originalRequest.newBuilder()
            .addHeader("auth", currAuth.accessToken)
            .addHeader("accept", "application/json")
            .build()

        return chain.proceed(authRequest)
    }

    private fun refreshToken(chain: Interceptor.Chain, expired: HKOAuth): HKOAuth = synchronized(this) {
        val currAuth = oauth ?: expired
        if (!currAuth.isExpired()) return@synchronized currAuth

        val refreshTokenResponse = chain.proceed(HikkaApi.refreshTokenRequest(currAuth.accessToken))
        if (!refreshTokenResponse.isSuccessful) {
            val code = refreshTokenResponse.code
            refreshTokenResponse.close()
            // Hikka answers an invalid or expired token with 400, so any client error except the retryable ones
            // means the token is dead, while timeouts, rate limits and server errors are transient
            if (code in 400..499 && code != 408 && code != 429) {
                hikka.logout()
                throw IOException("Hikka: The token is expired")
            }
            throw IOException("Hikka: Token check failed with HTTP $code")
        }
        refreshTokenResponse.close()

        val authTokenInfoResponse = chain.proceed(HikkaApi.authTokenInfo(currAuth.accessToken))
        if (!authTokenInfoResponse.isSuccessful) {
            authTokenInfoResponse.close()
            throw Exception("Hikka: Auth token info failed")
        }

        val authTokenInfo = with(json) {
            authTokenInfoResponse.parseAs<HKAuthTokenInfo>()
        }
        HKOAuth(currAuth.accessToken, authTokenInfo.expiration, authTokenInfo.created).also(::setAuth)
    }

    fun setAuth(oauth: HKOAuth?) {
        this.oauth = oauth
        hikka.saveOAuth(oauth)
    }
}
