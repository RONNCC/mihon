package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test
import java.io.IOException

class ExtensionStoreRestorerTest {

    private val repository = mockk<ExtensionStoreRepository>(relaxed = true)
    private val restorer = ExtensionStoreRestorer(repository)

    private fun backupStore(indexUrl: String) = BackupExtensionStore(
        indexUrl = indexUrl,
        name = "Store",
        badgeLabel = null,
        signingKey = "attacker",
        contactWebsite = "https://example.com",
        contactDiscord = null,
        isLegacy = false,
        extensionListUrl = null,
    )

    @Test
    fun `A store that is already added keeps its signing key`() = runTest {
        coEvery { repository.getAll() } returns listOf(
            ExtensionStore(
                indexUrl = "https://example.com/index.pb",
                name = "Store",
                badgeLabel = "Store",
                signingKey = "trusted",
                contact = ExtensionStore.Contact(website = "https://example.com", discord = null),
                isLegacy = false,
                extensionListUrl = null,
            ),
        )

        restorer(backupStore("https://example.com/index.pb"))

        coVerify(exactly = 0) { repository.upsert(any()) }
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `A new store is added from its index`() = runTest {
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.insert(any()) } returns Result.success(Unit)

        restorer(backupStore("https://example.com/index.pb"))

        coVerify { repository.insert("https://example.com/index.pb") }
        coVerify(exactly = 0) { repository.upsert(any()) }
    }

    @Test
    fun `A new store whose index can't be fetched is added without the backup's signing key`() = runTest {
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.insert(any()) } returns Result.failure(IOException())
        val restored = slot<ExtensionStore>()
        coEvery { repository.upsert(capture(restored)) } returns Unit

        restorer(backupStore("https://example.com/index.pb"))

        restored.captured.indexUrl shouldBe "https://example.com/index.pb"
        restored.captured.signingKey shouldBe ExtensionStore.NO_SIGNING_KEY
    }

    @Test
    fun `A store over plain HTTP is added from its index`() = runTest {
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.insert(any()) } returns Result.success(Unit)

        restorer(backupStore("http://example.com/index.pb"))

        coVerify { repository.insert("http://example.com/index.pb") }
        coVerify(exactly = 0) { repository.upsert(any()) }
    }
}
