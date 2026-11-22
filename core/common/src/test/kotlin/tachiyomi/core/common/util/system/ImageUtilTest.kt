package tachiyomi.core.common.util.system

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ImageUtilTest {

    @Test
    fun `Image extensions are recognised whatever their case`() {
        ImageUtil.isImage("001.JPG") shouldBe true
        ImageUtil.isImage("001.Png") shouldBe true
        ImageUtil.isImage("001.webp") shouldBe true
    }

    @Test
    fun `The jpeg extension is an image`() {
        ImageUtil.isImage("001.jpeg") shouldBe true
        ImageUtil.isImage("001.JPEG") shouldBe true
    }

    @Test
    fun `Other extensions are not images without their content`() {
        ImageUtil.isImage("ComicInfo.xml") shouldBe false
        ImageUtil.isImage("001") shouldBe false
        ImageUtil.isImage(null) shouldBe false
    }
}
