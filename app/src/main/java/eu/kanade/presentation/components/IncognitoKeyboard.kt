package eu.kanade.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.core.view.inputmethod.EditorInfoCompat
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState

/**
 * Asks the keyboard of every text field in [content] not to learn from the input while incognito mode is on, like
 * [eu.kanade.tachiyomi.util.view.setIncognito] does for views. Some keyboards may not respect it.
 *
 * Compose has no option for this, so the [android.view.inputmethod.EditorInfo] is changed on its way to the keyboard
 * (https://issuetracker.google.com/issues/359257538).
 */
@Composable
fun IncognitoKeyboard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val incognitoMode = remember { context.appGraph.basePreferences.incognitoMode }
    val incognito by incognitoMode.collectAsState()

    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            if (!incognito) {
                nextHandler.startInputMethod(request)
            } else {
                nextHandler.startInputMethod(
                    PlatformTextInputMethodRequest { outAttributes ->
                        request.createInputConnection(outAttributes).also {
                            outAttributes.imeOptions =
                                outAttributes.imeOptions or EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
                        }
                    },
                )
            }
        },
        content = content,
    )
}
