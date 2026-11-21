package mihon.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

/**
 * Mullvad shut down its public DNS over HTTPS servers, so anyone still using them goes back to the system DNS.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class MullvadDohMigration(
    private val networkPreferences: NetworkPreferences,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        if (networkPreferences.dohProvider.get() == REMOVED_MULLVAD_PROVIDER) {
            networkPreferences.dohProvider.set(-1)
        }
        return true
    }
}

private const val REMOVED_MULLVAD_PROVIDER = 9
