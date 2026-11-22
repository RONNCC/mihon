package eu.kanade.tachiyomi.ui.base.delegate

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.app.di.appGraph

interface ThemingDelegate {
    fun applyAppTheme(activity: AppCompatActivity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_Tachiyomi))
                if (isAmoled) add(R.style.ThemeOverlay_Tachiyomi_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: AppCompatActivity) {
        val uiPreferences = activity.appGraph.uiPreferences
        val applied = uiPreferences.appTheme.get() to uiPreferences.themeDarkAmoled.get()
        ThemingDelegate.getThemeResIds(applied.first, applied.second).forEach(activity::setTheme)

        // The theme is only read on creation, so a change made elsewhere, like by a restore, needs a recreation
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(uiPreferences.appTheme.changes(), uiPreferences.themeDarkAmoled.changes(), ::Pair)
                    .first { it != applied }
                ActivityCompat.recreate(activity)
            }
        }
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_Tachiyomi_Monet,
    AppTheme.CATPPUCCIN to R.style.Theme_Tachiyomi_Catppuccin,
    AppTheme.TOKYONIGHT to R.style.Theme_Tachiyomi_TokyoNight,
    AppTheme.GREEN_APPLE to R.style.Theme_Tachiyomi_GreenApple,
    AppTheme.LAVENDER to R.style.Theme_Tachiyomi_Lavender,
    AppTheme.MIDNIGHT_DUSK to R.style.Theme_Tachiyomi_MidnightDusk,
    AppTheme.MONOCHROME to R.style.Theme_Tachiyomi_Monochrome,
    AppTheme.NORD to R.style.Theme_Tachiyomi_Nord,
    AppTheme.STRAWBERRY_DAIQUIRI to R.style.Theme_Tachiyomi_StrawberryDaiquiri,
    AppTheme.TAKO to R.style.Theme_Tachiyomi_Tako,
    AppTheme.TEALTURQUOISE to R.style.Theme_Tachiyomi_TealTurquoise,
    AppTheme.YINYANG to R.style.Theme_Tachiyomi_YinYang,
    AppTheme.YOTSUBA to R.style.Theme_Tachiyomi_Yotsuba,
    AppTheme.TIDAL_WAVE to R.style.Theme_Tachiyomi_TidalWave,
)
