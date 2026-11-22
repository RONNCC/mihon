package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class PagerItemsTest {

    private val items = PagerItems(isReversed = false)

    private fun chapter(id: Long, pageCount: Int): ReaderChapter {
        val chapter = ReaderChapter(Chapter.create().copy(id = id))
        val pages = List(pageCount) { ReaderPage(it).also { page -> page.chapter = chapter } }
        chapter.state = ReaderChapter.State.Loaded(pages)
        return chapter
    }

    @Test
    fun `Reading forward into a one page chapter stays on the first half of its split page`() {
        val first = chapter(1, pageCount = 3)
        val second = chapter(2, pageCount = 1)
        val page = second.pages!!.single()
        items.setChapters(ViewerChapters(first, null, second), forceTransition = false, currentPosition = 0)
        items.onPageSplit(page, InsertPage(page))

        val update = items.setChapters(
            ViewerChapters(second, first, null),
            forceTransition = false,
            currentPosition = items.items.indexOf(page),
        )

        update.insertPage shouldBe null
    }

    @Test
    fun `Reading back onto a split last page moves to its second half`() {
        val first = chapter(1, pageCount = 3)
        val second = chapter(2, pageCount = 2)
        val lastPage = first.pages!!.last()
        items.setChapters(ViewerChapters(second, first, null), forceTransition = false, currentPosition = 0)
        val split = InsertPage(lastPage)
        items.onPageSplit(lastPage, split)

        val update = items.setChapters(
            ViewerChapters(first, null, second),
            forceTransition = false,
            currentPosition = items.items.indexOf(lastPage),
        )

        update.insertPage shouldBe split
    }
}
