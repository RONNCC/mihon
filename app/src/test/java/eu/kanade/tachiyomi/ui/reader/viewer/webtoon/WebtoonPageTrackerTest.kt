package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class WebtoonPageTrackerTest {

    private val activity = mockk<ReaderActivity>(relaxed = true)
    private val items = WebtoonItems()
    private val tracker = WebtoonPageTracker(activity, items, hideThreshold = 0)

    private fun chapter(id: Long, pageCount: Int): ReaderChapter {
        val chapter = ReaderChapter(Chapter.create().copy(id = id))
        val pages = List(pageCount) { ReaderPage(it).also { page -> page.chapter = chapter } }
        chapter.state = ReaderChapter.State.Loaded(pages)
        return chapter
    }

    @Test
    fun `A jump past the last page to its transition selects the last page`() {
        val current = chapter(1, pageCount = 3)
        items.setChapters(ViewerChapters(current, null, chapter(2, pageCount = 2)), forceTransition = true)
        val pages = current.pages!!
        val nextTransition = items.items.indexOfFirst { it is ChapterTransition.Next }

        tracker.onScrolled(items.items.indexOf(pages[0]))
        tracker.onScrolled(nextTransition)

        verifyOrder {
            activity.onPageSelected(pages[0])
            activity.onPageSelected(pages[2])
        }
    }

    @Test
    fun `A jump past the last page to the next chapter selects the last page`() {
        val current = chapter(1, pageCount = 3)
        val next = chapter(2, pageCount = 2)
        items.setChapters(ViewerChapters(current, null, next), forceTransition = false)
        val pages = current.pages!!

        tracker.onScrolled(items.items.indexOf(pages[0]))
        tracker.onScrolled(items.items.indexOf(next.pages!![0]))

        verifyOrder {
            activity.onPageSelected(pages[0])
            activity.onPageSelected(pages[2])
            activity.onPageSelected(next.pages!![0])
        }
    }

    @Test
    fun `Jumping within a chapter does not select its last page`() {
        val current = chapter(1, pageCount = 4)
        items.setChapters(ViewerChapters(current, null, null), forceTransition = false)
        val pages = current.pages!!

        tracker.onScrolled(items.items.indexOf(pages[0]))
        tracker.onScrolled(items.items.indexOf(pages[2]))

        verify(exactly = 0) { activity.onPageSelected(pages[3]) }
    }
}
