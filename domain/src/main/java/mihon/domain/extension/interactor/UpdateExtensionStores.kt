package mihon.domain.extension.interactor

import dev.zacsweers.metro.Inject
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository

@Inject
class UpdateExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke(): List<ExtensionStore> {
        return repository.refreshAll()
    }
}
