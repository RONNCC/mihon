package tachiyomi.domain.history.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

@Inject
class GetHistory(
    private val repository: HistoryRepository,
) {

    suspend fun await(mangaId: Long): List<History> {
        return repository.getHistoryByMangaId(mangaId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        // Matched here since SQLite only folds the case of ASCII letters and treats "%" and "_" in a query as wildcards
        return repository.getHistory().map { history ->
            history.filter { it.title.contains(query, ignoreCase = true) }
        }
    }
}
