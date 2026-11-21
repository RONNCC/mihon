package mihon.core.migration.migrations

import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mihon.core.migration.MigrationContext
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class MullvadDohMigrationTest {

    private val networkPreferences = NetworkPreferences(InMemoryPreferenceStore(), isDebugBuild = false)
    private val migration = MullvadDohMigration(networkPreferences)

    @Test
    fun `Mullvad goes back to the system DNS`() = runBlocking<Unit> {
        networkPreferences.dohProvider.set(9)

        migration(MigrationContext(dryrun = false, previousVersion = 39)) shouldBe true

        networkPreferences.dohProvider.get() shouldBe -1
    }

    @Test
    fun `Other providers are kept`() = runBlocking<Unit> {
        networkPreferences.dohProvider.set(PREF_DOH_CLOUDFLARE)

        migration(MigrationContext(dryrun = false, previousVersion = 39)) shouldBe true

        networkPreferences.dohProvider.get() shouldBe PREF_DOH_CLOUDFLARE
    }
}
