package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ArchivePageLoaderTest {

    @Test
    fun `Loading a page after the loader is recycled does not throw`() = runTest {
        val loader = ArchivePageLoader(mockk(relaxed = true))

        loader.recycle()

        // A viewer being replaced can still ask for a page of the chapter it just released
        loader.loadPage(ReaderPage(0))
    }
}
