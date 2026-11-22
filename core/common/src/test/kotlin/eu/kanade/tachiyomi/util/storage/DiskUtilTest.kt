package eu.kanade.tachiyomi.util.storage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class DiskUtilTest {

    @Test
    fun `characters outside the BMP stay distinct when non ASCII is disallowed`() {
        val smile = DiskUtil.buildValidFilename("a\uD83D\uDE00", disallowNonAscii = true)
        val grin = DiskUtil.buildValidFilename("a\uD83D\uDE01", disallowNonAscii = true)

        smile shouldBe "af09f9880"
        grin shouldBe "af09f9881"
        smile shouldNotBe grin
    }

    @Test
    fun `BMP characters are encoded one by one`() {
        DiskUtil.buildValidFilename("é翻", disallowNonAscii = true) shouldBe "c3a9e7bfbb"
    }

    @Test
    fun `names given before code points were encoded whole can be reproduced`() {
        DiskUtil.buildValidFilename("\uD83D\uDE00", disallowNonAscii = true, hexUtf16Units = true) shouldBe "3f3f"
        DiskUtil.buildValidFilename("\uD83D\uDE01", disallowNonAscii = true, hexUtf16Units = true) shouldBe "3f3f"
    }

    @Test
    fun `non ASCII characters are kept when allowed`() {
        DiskUtil.buildValidFilename("a\uD83D\uDE00/é") shouldBe "a\uD83D\uDE00_é"
    }
}
