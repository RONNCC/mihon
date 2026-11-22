package eu.kanade.tachiyomi.data.track

import tachiyomi.core.common.preference.Preference
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.minutes

/**
 * A tracker's pending OAuth login. Login results reach the app through an exported deep link, so one is only
 * accepted while a login the user started is pending, which stops other apps and sites from logging the user into
 * their own account.
 */
class OAuthLoginState(
    private val preference: Preference<String>,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Starts a login, replacing any pending one, and returns the `state` to send to the provider. */
    fun start(): String {
        val bytes = ByteArray(16).also(SecureRandom()::nextBytes)
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        preference.set("$state ${now() + TIMEOUT.inWholeMilliseconds}")
        return state
    }

    /** Ends the pending login if [state] is the one it was started with. */
    fun finish(state: String?): Boolean = finish { it == state }

    /** Ends the pending login, for providers that don't send the `state` back. */
    fun finishWithoutState(): Boolean = finish { true }

    private fun finish(matches: (String) -> Boolean): Boolean {
        val (pendingState, expiresAt) = preference.get().split(' ').takeIf { it.size == 2 } ?: return false
        if (now() > (expiresAt.toLongOrNull() ?: 0)) {
            preference.delete()
            return false
        }
        if (!matches(pendingState)) return false
        preference.delete()
        return true
    }

    private companion object {
        val TIMEOUT = 15.minutes
    }
}
