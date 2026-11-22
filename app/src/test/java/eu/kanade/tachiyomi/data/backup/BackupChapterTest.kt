package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BackupChapterTest {

    @Test
    fun `Chapter number restores as written, not as its float expansion`() {
        BackupChapter(url = "/c", name = "c", chapterNumber = 10.1f).toChapterImpl().chapterNumber shouldBe 10.1
    }
}
