package eu.kanade.tachiyomi.ui.deeplink

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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private fun viewModel(sourceManager: SourceManager) =
        DeepLinkViewModel("https://example.org/manga/1", sourceManager, mockk(), mockk(), mockk())

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
}
