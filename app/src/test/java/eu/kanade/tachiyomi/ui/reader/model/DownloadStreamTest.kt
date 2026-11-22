package eu.kanade.tachiyomi.ui.reader.model

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.io.IOException

class DownloadStreamTest {

    @Test
    fun `A download announced over the limit is not kept`() {
        val stream = DownloadStream()
        val reader = stream.reader()

        stream.reserve((64L shl 20) + 1)
        stream.append(ByteArray(16), 0, 16)
        stream.finish()

        shouldThrow<IOException> { reader.read(ByteArray(16), 0, 16) }
    }
}
