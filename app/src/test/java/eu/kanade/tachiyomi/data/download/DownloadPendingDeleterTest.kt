package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

class DownloadPendingDeleterTest {

    private val store = mutableMapOf<String, String>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true).also { editor ->
        every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg()
            editor
        }
        every { editor.clear() } answers {
            store.clear()
            editor
        }
    }
    private val preferences = mockk<SharedPreferences> {
        every { all } answers { store.toMap() }
        every { getString(any(), any()) } answers { store[firstArg()] }
        every { edit() } returns editor
    }
    private val context = mockk<Context> {
        every { getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE) } returns preferences
    }
    private val deleter = DownloadPendingDeleter(context, Json)

    private val manga = Manga.create().copy(id = 1, source = 2, title = "Manga")
    private fun chapter(id: Long) = Chapter.create().copy(id = id, mangaId = manga.id, name = "Chapter $id")

    @Test
    fun `chapters kept stay pending for the next call`() {
        deleter.addChapters(listOf(chapter(10), chapter(11)), manga)

        val first = deleter.getPendingChapters(keeping = setOf(11L))
        val second = deleter.getPendingChapters()

        assertEquals(listOf(10L), first.values.single().map { it.id })
        assertEquals(listOf(11L), second.values.single().map { it.id })
        assertEquals(emptyMap<Manga, List<Chapter>>(), deleter.getPendingChapters())
    }

    @Test
    fun `a manga with only kept chapters is not returned`() {
        deleter.addChapters(listOf(chapter(10)), manga)

        assertEquals(emptyMap<Manga, List<Chapter>>(), deleter.getPendingChapters(keeping = setOf(10L)))
        assertEquals(listOf(10L), deleter.getPendingChapters().values.single().map { it.id })
    }
}
