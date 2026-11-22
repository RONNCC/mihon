package mihon.domain.extension.repository

import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.model.ExtensionListing
import mihon.domain.extension.model.ExtensionStore

interface ExtensionStoreRepository {
    suspend fun insert(indexUrl: String): Result<Unit>

    suspend fun insertFromPreference(indexUrl: String, name: String)

    suspend fun upsert(store: ExtensionStore)

    /**
     * Refreshes the stores from their indexes, except those whose signing key changed, which are kept as they were.
     *
     * @return the stores whose signing key changed.
     */
    suspend fun refreshAll(): List<ExtensionStore>

    /**
     * @throws java.io.IOException when there are stores but none of them could be reached.
     */
    suspend fun fetchExtensions(): ExtensionListing

    suspend fun getAll(): List<ExtensionStore>

    fun getAllAsFlow(): Flow<List<ExtensionStore>>

    fun getCountAsFlow(): Flow<Long>

    suspend fun remove(indexUrl: String)
}
