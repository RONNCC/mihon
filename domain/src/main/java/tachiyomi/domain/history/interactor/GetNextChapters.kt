package tachiyomi.domain.history.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.interactor.GetManga
import kotlin.math.max

@Inject
class GetNextChapters(
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val getManga: GetManga,
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(onlyUnread: Boolean = true): List<Chapter> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.mangaId, history.chapterId, onlyUnread)
    }

    suspend fun await(mangaId: Long, onlyUnread: Boolean = true): List<Chapter> {
        val manga = getManga.await(mangaId) ?: return emptyList()
        val chapters = getChaptersByMangaId.await(mangaId, applyScanlatorFilter = true)
            .sortedWith(getChapterSort(manga, sortDescending = false))

        return if (onlyUnread) {
            chapters.filterNot { it.read }
        } else {
            chapters
        }
    }

    suspend fun await(
        mangaId: Long,
        fromChapterId: Long,
        onlyUnread: Boolean = true,
    ): List<Chapter> {
        val chapters = await(mangaId, onlyUnread)
        val currChapterIndex = chapters.indexOfFirst { it.id == fromChapterId }
        if (currChapterIndex == -1 && !onlyUnread) return chaptersAfterHidden(mangaId, fromChapterId, chapters)
        val nextChapters = chapters.subList(max(0, currChapterIndex), chapters.size)

        if (onlyUnread) {
            return nextChapters
        }

        // The "next chapter" is either:
        // - The current chapter if it isn't completely read
        // - The chapters after the current chapter if the current one is completely read
        val fromChapter = chapters[currChapterIndex]
        return if (!fromChapter.read) nextChapters else nextChapters.drop(1)
    }

    /**
     * The scanlator filter hides the chapter to resume from, so this goes on from where it would be among the
     * [chapters] that are shown: from its number if it wasn't read, after it if it was.
     */
    private suspend fun chaptersAfterHidden(
        mangaId: Long,
        fromChapterId: Long,
        chapters: List<Chapter>,
    ): List<Chapter> {
        val manga = getManga.await(mangaId) ?: return chapters
        val all = getChaptersByMangaId.await(mangaId, applyScanlatorFilter = false)
            .sortedWith(getChapterSort(manga, sortDescending = false))
        val hiddenIndex = all.indexOfFirst { it.id == fromChapterId }
        if (hiddenIndex == -1) return chapters

        val hidden = all[hiddenIndex]
        val positions = all.withIndex().associate { (index, chapter) -> chapter.id to index }
        val next = chapters.indexOfFirst {
            positions.getValue(it.id) > hiddenIndex ||
                (!hidden.read && hidden.isRecognizedNumber && it.chapterNumber == hidden.chapterNumber)
        }
        return if (next == -1) emptyList() else chapters.subList(next, chapters.size)
    }
}
