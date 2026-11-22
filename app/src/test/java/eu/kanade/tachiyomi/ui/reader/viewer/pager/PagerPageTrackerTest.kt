package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class PagerPageTrackerTest {

    private val items = PagerItems(isReversed = false)
    private val selected = mutableListOf<Pair<ReaderPage, Boolean>>()
    private val tracker = PagerPageTracker(mockk<ReaderActivity>(relaxed = true), items, {}) { page, forward ->
        selected += page to forward
    }

    private val first = chapter(1, pageCount = 3)
    private val second = chapter(2, pageCount = 2)

    init {
        items.setChapters(ViewerChapters(second, first, null), forceTransition = true, currentPosition = 0)
    }

    private fun chapter(id: Long, pageCount: Int): ReaderChapter {
        val chapter = ReaderChapter(Chapter.create().copy(id = id))
        val pages = List(pageCount) { ReaderPage(it).also { page -> page.chapter = chapter } }
        chapter.state = ReaderChapter.State.Loaded(pages)
        return chapter
    }

    private fun moveTo(item: Any) = tracker.onPageChange(items.items.indexOf(item))

    @Test
    fun `Leaving the previous transition into its chapter goes forward`() {
        moveTo(items.items.single { it is ChapterTransition.Prev })
        moveTo(second.pages!!.first())

        selected.last() shouldBe (second.pages!!.first() to true)
    }

    @Test
    fun `Going back from the next transition onto the last page goes backward`() {
        moveTo(items.items.single { it is ChapterTransition.Next })
        moveTo(second.pages!!.last())

        selected.last() shouldBe (second.pages!!.last() to false)
    }

    @Test
    fun `Going back onto the previous chapter's last page goes backward`() {
        moveTo(second.pages!!.first())
        moveTo(first.pages!!.last())

        selected.last() shouldBe (first.pages!!.last() to false)
    }
}
