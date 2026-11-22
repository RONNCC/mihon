package mihon.core.migration.migrations

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import java.io.File

/**
 * The chapter cache moved to `chapter_cache`, and nothing reads or evicts the folder the old one left behind.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class LegacyChapterCacheMigration(
    private val context: Context,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        File(context.cacheDir, LEGACY_CHAPTER_CACHE).deleteRecursively()
        return true
    }
}

private const val LEGACY_CHAPTER_CACHE = "chapter_disk_cache"
