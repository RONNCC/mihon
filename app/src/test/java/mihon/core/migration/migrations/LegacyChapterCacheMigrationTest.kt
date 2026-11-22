package mihon.core.migration.migrations

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mihon.core.migration.MigrationContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LegacyChapterCacheMigrationTest {

    @TempDir
    lateinit var cacheDir: File

    private val context = mockk<Context>()

    private fun migration() = LegacyChapterCacheMigration(context.also { every { it.cacheDir } returns cacheDir })

    @Test
    fun `the legacy cache is deleted and the current one is kept`() = runBlocking<Unit> {
        val legacy = File(cacheDir, "chapter_disk_cache")
        val current = File(cacheDir, "chapter_cache")
        File(legacy, "page.0").apply { parentFile!!.mkdirs() }.writeText("x")
        File(current, "page").apply { parentFile!!.mkdirs() }.writeText("x")

        migration()(MigrationContext(dryrun = false, previousVersion = 39)) shouldBe true

        legacy.exists() shouldBe false
        current.exists() shouldBe true
    }

    @Test
    fun `nothing to delete is fine`() = runBlocking<Unit> {
        migration()(MigrationContext(dryrun = false, previousVersion = 39)) shouldBe true
    }
}
