package eu.kanade.tachiyomi.data.backup

import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class AutoBackupRotationTest {

    private fun backup(name: String) = mockk<UniFile> {
        every { this@mockk.name } returns name
        every { uri } returns mockk<Uri>()
    }

    @Test
    fun `keeps the newest backups and the current one`() {
        val old = listOf("a_2026-01-01_00-00", "a_2026-01-02_00-00", "a_2026-01-03_00-00", "a_2026-01-04_00-00")
            .map(::backup)
        val current = backup("a_2026-01-05_00-00")

        val toDelete = BackupCreator.getBackupsToDelete(old + current, current)

        toDelete.map { it.name } shouldBe listOf("a_2026-01-01_00-00")
    }

    @Test
    fun `never deletes the current backup even when the clock went backwards`() {
        val old = listOf("a_2026-01-02_00-00", "a_2026-01-03_00-00", "a_2026-01-04_00-00", "a_2026-01-05_00-00")
            .map(::backup)
        val current = backup("a_2026-01-01_00-00")

        val toDelete = BackupCreator.getBackupsToDelete(old + current, current)

        toDelete.map { it.name } shouldBe listOf("a_2026-01-02_00-00")
    }
}
