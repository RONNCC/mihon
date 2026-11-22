package eu.kanade.tachiyomi.ui.setting.track

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.comick.ComickMissingScopesException
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR

class TrackLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(uri: Uri) {
        val data = when {
            !uri.encodedQuery.isNullOrBlank() -> uri.encodedQuery
            !uri.encodedFragment.isNullOrBlank() -> uri.encodedFragment
            else -> null
        }
            ?.split("&")
            ?.filter { it.isNotBlank() }
            ?.associate {
                val parts = it.split("=", limit = 2).map<String, String>(Uri::decode)
                parts[0] to parts.getOrNull(1)
            }
            .orEmpty()
        val state = data["state"]

        lifecycleScope.launch {
            with(trackerManager) {
                // Hikka doesn't send the state back, and AniList and Bangumi aren't known to always do, e.g. when the
                // user has to sign in first, so for them a pending login is enough
                when (uri.host) {
                    "anilist-auth" -> login(aniList, data["access_token"], state, requireState = false) {
                        aniList.login(it)
                    }
                    "comick-auth" -> login(comick, data["code"], state) { loginComick(it) }
                    "bangumi-auth" -> login(bangumi, data["code"], state, requireState = false) { bangumi.login(it) }
                    "mangabaka-auth" -> login(mangaBaka, data["code"], state) { mangaBaka.login(it) }
                    "myanimelist-auth" -> login(myAnimeList, data["code"], state) { myAnimeList.login(it) }
                    "shikimori-auth" -> login(shikimori, data["code"], state) { shikimori.login(it) }
                    "hikka-auth" -> login(hikka, data["reference"], state, requireState = false) { hikka.login(it) }
                }
            }
            returnToSettings()
        }
    }

    /**
     * Logs in with [code] if it answers the login the user started. Anything can open this activity, so other
     * results, like a denied login, are ignored rather than logging out.
     */
    private suspend fun login(
        tracker: BaseTracker,
        code: String?,
        state: String?,
        requireState: Boolean = true,
        loginWith: suspend (String) -> Unit,
    ) {
        if (code != null && tracker.oauthLoginState.finish(state, requireState)) {
            loginWith(code)
            return
        }
        logcat(LogPriority.WARN) { "Ignored a ${tracker.name} login result without a code or a pending login" }
        toast(MR.strings.login_failed)
    }

    private suspend fun loginComick(code: String) {
        try {
            trackerManager.comick.login(code)
        } catch (e: ComickMissingScopesException) {
            toast(e.message, Toast.LENGTH_LONG)
        }
    }
}
