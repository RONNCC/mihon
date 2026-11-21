package eu.kanade.tachiyomi.util.chapter

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga

/**
 * Returns the chapter [slots] positions before [currentChapter] in reading order, or null if there is none.
 *
 * This takes all the chapters of the manga, not the reader's list, which leaves out read, filtered and duplicate
 * chapters and would put the position on a chapter the user didn't mean. [currentChapter] replaces its stored row,
 * which is written after this runs and may be missing from the list when a scanlator filter hides it.
 */
fun List<Chapter>.getChapterToDeleteAfterRead(
    manga: Manga,
    currentChapter: Chapter,
    slots: Int,
    skipDuplicates: Boolean,
): Chapter? {
    val chapters = if (any { it.id == currentChapter.id }) {
        map { if (it.id == currentChapter.id) currentChapter else it }
    } else {
        this + currentChapter
    }
    val sorted = chapters
        .sortedWith(getChapterSort(manga, sortDescending = false))
        .let { if (skipDuplicates) it.removeDuplicates(currentChapter) else it }
    return sorted.getOrNull(sorted.indexOfFirst { it.id == currentChapter.id } - slots)
}
