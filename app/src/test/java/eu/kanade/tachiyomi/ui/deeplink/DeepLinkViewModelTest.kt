package eu.kanade.tachiyomi.ui.deeplink

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ResolvableSource
import eu.kanade.tachiyomi.source.online.UriType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.domain.manga.model.toDomainManga
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkViewModelTest {

    @BeforeEach
    fun setMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        sourceManager: SourceManager,
        networkToLocalManga: NetworkToLocalManga = mockk(),
    ) = DeepLinkViewModel("https://example.org/manga/1", sourceManager, networkToLocalManga, mockk(), mockk())

    @Test
    fun `a failing source resolves to no results`() = runTest {
        val source = mockk<ResolvableSource>()
        every { source.getUriType(any()) } returns UriType.Manga
        coEvery { source.getManga(any()) } throws IOException("offline")
        val sourceManager = mockk<SourceManager>()
        coEvery { sourceManager.getAll() } returns listOf(source)

        val state = viewModel(sourceManager).state.first { it !is DeepLinkViewModel.State.Loading }

        assertEquals(DeepLinkViewModel.State.NoResults, state)
    }

    @Test
    fun `no resolvable source resolves to no results`() = runTest {
        val sourceManager = mockk<SourceManager>()
        coEvery { sourceManager.getAll() } returns emptyList()

        val state = viewModel(sourceManager).state.first { it !is DeepLinkViewModel.State.Loading }

        assertEquals(DeepLinkViewModel.State.NoResults, state)
    }

    @Test
    fun `a failing chapter lookup still resolves the manga`() = runTest {
        val sManga = SManga.create().apply {
            url = "/manga/1"
            title = "Manga"
        }
        val source = mockk<ResolvableSource>()
        every { source.id } returns 1L
        every { source.getUriType(any()) } returns UriType.Chapter
        coEvery { source.getManga(any()) } returns sManga
        coEvery { source.getChapter(any()) } throws IOException("offline")
        val sourceManager = mockk<SourceManager>()
        coEvery { sourceManager.getAll() } returns listOf(source)
        val manga = sManga.toDomainManga(1L).copy(id = 5L)
        val networkToLocalManga = mockk<NetworkToLocalManga>()
        coEvery { networkToLocalManga(any<Manga>()) } returns manga

        val state = viewModel(sourceManager, networkToLocalManga).state.first {
            it !is DeepLinkViewModel.State.Loading
        }

        assertEquals(DeepLinkViewModel.State.Result(manga), state)
    }
}
