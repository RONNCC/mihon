package eu.kanade.tachiyomi.ui.reader

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderActivityInsetsTest {

    @Test
    fun `The cutout is avoided outside fullscreen whatever the draw under cutout default is`() {
        avoidsDisplayCutout(fullscreen = false, drawUnderCutout = true) shouldBe true
        avoidsDisplayCutout(fullscreen = false, drawUnderCutout = false) shouldBe true
    }

    @Test
    fun `The cutout is avoided in fullscreen only when drawing under it is off`() {
        avoidsDisplayCutout(fullscreen = true, drawUnderCutout = true) shouldBe false
        avoidsDisplayCutout(fullscreen = true, drawUnderCutout = false) shouldBe true
    }

    @Test
    fun `The system bar icons contrast with the reader background while the menu is hidden`() {
        systemBarsHaveDarkIcons(menuVisible = false, isDarkTheme = true, background = Color.White) shouldBe true
        systemBarsHaveDarkIcons(menuVisible = false, isDarkTheme = false, background = Color.Black) shouldBe false
        systemBarsHaveDarkIcons(menuVisible = false, isDarkTheme = false, background = Color(0xFF202125)) shouldBe false
    }

    @Test
    fun `The system bar icons follow the app theme while the menu shows`() {
        systemBarsHaveDarkIcons(menuVisible = true, isDarkTheme = false, background = Color.Black) shouldBe true
        systemBarsHaveDarkIcons(menuVisible = true, isDarkTheme = true, background = Color.White) shouldBe false
    }
}
