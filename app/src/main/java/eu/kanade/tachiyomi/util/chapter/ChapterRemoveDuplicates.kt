package eu.kanade.tachiyomi.util.chapter

import tachiyomi.domain.chapter.model.Chapter

/**
 * Returns a copy of the sorted list keeping one chapter of each run of neighbours sharing a recognised number.
 * The same number further on, like a new season starting again at 1, is another chapter.
 */
fun List<Chapter>.removeDuplicates(currentChapter: Chapter): List<Chapter> {
    val runs = mutableListOf<MutableList<Chapter>>()
    forEach { chapter ->
        val run = runs.lastOrNull()
        if (run != null && chapter.isRecognizedNumber && run.first().chapterNumber == chapter.chapterNumber) {
            run += chapter
        } else {
            runs += mutableListOf(chapter)
        }
    }
    return runs.map { chapters ->
        chapters.find { it.id == currentChapter.id }
            ?: chapters.find { it.scanlator == currentChapter.scanlator }
            ?: chapters.first()
    }
}
