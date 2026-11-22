package tachiyomi.domain.manga.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.repository.MangaRepository

@Inject
class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): List<MangaWithChapterCount> {
        // Matched here since SQLite only folds the case of ASCII letters and treats "%" and "_" in a title as wildcards
        val titleMatchIds = if (manga.title.isBlank()) {
            emptyList()
        } else {
            mangaRepository.getFavoriteTitles()
                .filter { it.id != manga.id && it.title.contains(manga.title, ignoreCase = true) }
                .map { it.id }
        }
        return mangaRepository.getDuplicateLibraryManga(manga.id, titleMatchIds)
    }
}
