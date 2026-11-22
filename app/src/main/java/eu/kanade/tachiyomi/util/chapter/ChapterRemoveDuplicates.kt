package eu.kanade.tachiyomi.util.chapter

import tachiyomi.domain.chapter.model.Chapter

/**
 * Returns a copy of the sorted list keeping one chapter of each recognised number, merging a chapter into the
 * latest one before it with its number when that is its neighbour or from other scanlators. The same number
 * further on from the same scanlator, like a new season starting again at 1, is another chapter.
 */
fun List<Chapter>.removeDuplicates(currentChapter: Chapter): List<Chapter> {
    val groups = mutableListOf<MutableList<Chapter>>()
    val latestByNumber = mutableMapOf<Double, MutableList<Chapter>>()
    forEach { chapter ->
        val earlier = latestByNumber[chapter.chapterNumber]?.takeIf { chapter.isRecognizedNumber }
        if (earlier != null && (earlier === groups.last() || earlier.none { it.scanlator == chapter.scanlator })) {
            earlier += chapter
        } else {
            val group = mutableListOf(chapter)
            groups += group
            latestByNumber[chapter.chapterNumber] = group
        }
    }
    return groups.map { chapters ->
        chapters.find { it.id == currentChapter.id }
            ?: chapters.find { it.scanlator == currentChapter.scanlator }
            ?: chapters.first()
    }
}
