package eu.kanade.tachiyomi.ui.reader

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
}
