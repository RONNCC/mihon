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

    @Test
    fun `Removing split pages keeps the page whose half is shown`() {
        val current = chapter(1, pageCount = 4)
        val pages = current.pages!!
        items.setChapters(ViewerChapters(current, null, null), forceTransition = false, currentPosition = 0)
        items.onPageSplit(pages[0], InsertPage(pages[0]))
        val secondHalf = InsertPage(pages[2])
        items.onPageSplit(pages[2], secondHalf)

        val position = items.cleanupPageSplit(items.items.indexOf(secondHalf))

        items.items[position] shouldBe pages[2]
    }

    @Test
    fun `Removing split pages drops those of adjacent chapters`() {
        val first = chapter(1, pageCount = 2)
        val second = chapter(2, pageCount = 2)
        val page = second.pages!!.first()
        items.setChapters(ViewerChapters(first, null, second), forceTransition = false, currentPosition = 0)
        items.onPageSplit(page, InsertPage(page))

        items.cleanupPageSplit(0)
        items.setChapters(
            ViewerChapters(second, first, null),
            forceTransition = false,
            currentPosition = items.items.indexOf(page),
        )

        items.items.filterIsInstance<InsertPage>() shouldBe emptyList()
    }
}
