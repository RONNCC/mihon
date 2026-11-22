package eu.kanade.tachiyomi.data.backup.restore.restorers

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository

@Inject
class ExtensionStoreRestorer(
    private val extensionStoreRepository: ExtensionStoreRepository,
) {

    /**
     * Adds the store if it isn't added yet. A backup can hold any signing key, so the key comes from the store's
     * index, like for a store added by hand, and an added store keeps its own. When the index can't be fetched now,
     * the store is added without a key, which its first refresh fills in.
     */
    suspend operator fun invoke(
        backupStore: BackupExtensionStore,
    ) {
        if (extensionStoreRepository.getAll().any { it.indexUrl == backupStore.indexUrl }) return
        if (extensionStoreRepository.insert(backupStore.indexUrl).isSuccess) return

        extensionStoreRepository.upsert(
            ExtensionStore(
                indexUrl = backupStore.indexUrl,
                name = backupStore.name,
                badgeLabel = backupStore.badgeLabel ?: backupStore.name,
                signingKey = ExtensionStore.NO_SIGNING_KEY,
                contact = ExtensionStore.Contact(
                    website = backupStore.contactWebsite,
                    discord = backupStore.contactDiscord,
                ),
                isLegacy = backupStore.isLegacy ?: true,
                extensionListUrl = backupStore.extensionListUrl,
            ),
        )
    }
}
