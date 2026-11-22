package tachiyomi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.chapter.model.Chapter

@Inject
class ShouldUpdateDbChapter {

    fun await(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            // A source without an upload date keeps the stored one, so a missing date is never a change
            (sourceChapter.dateUpload != null && dbChapter.dateUpload != sourceChapter.dateUpload) ||
            dbChapter.chapterNumber != sourceChapter.chapterNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder ||
            dbChapter.memo != sourceChapter.memo
    }
}
