package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadStoreTest {

    @Test
    fun `restore keeps the chapters queued while it was waiting`() = runTest(UnconfinedTestDispatcher()) {
        val source = mockk<HttpSource>()
        val manga = Manga.create().copy(id = 1, source = 2)
        val restored = Chapter.create().copy(id = 10, mangaId = manga.id)
        val queuedMeanwhile = Chapter.create().copy(id = 11, mangaId = manga.id)
        val extensionsLoaded = CompletableDeferred<Unit>()
        val sourceManager = mockk<SourceManager>()
        val getManga = mockk<GetManga>()
        val getChapter = mockk<GetChapter>()
        coEvery { sourceManager.get(manga.source) } coAnswers {
            extensionsLoaded.await()
            source
        }
        coEvery { getManga.await(manga.id) } returns manga
        coEvery { getChapter.await(restored.id) } returns restored
        val context = mockk<Context>()
        val preferences = FakeSharedPreferences()
        every { context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE) } returns preferences
        val store = DownloadStore(context, sourceManager, Json, getManga, getChapter)
        store.addAll(listOf(Download(source, manga, restored)))

        val restoring = async { store.restore() }
        store.addAll(listOf(Download(source, manga, queuedMeanwhile)))
        extensionsLoaded.complete(Unit)

        assertEquals(listOf(restored.id), restoring.await().map { it.chapter.id })
        assertEquals(setOf(queuedMeanwhile.id.toString()), preferences.all.keys)
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?) = throw NotImplementedError()
    override fun getInt(key: String, defValue: Int) = throw NotImplementedError()
    override fun getLong(key: String, defValue: Long) = throw NotImplementedError()
    override fun getFloat(key: String, defValue: Float) = throw NotImplementedError()
    override fun getBoolean(key: String, defValue: Boolean) = throw NotImplementedError()
    override fun contains(key: String) = key in values
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = throw NotImplementedError()
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = throw NotImplementedError()

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var cleared = false

        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun remove(key: String) = apply { removed += key }
        override fun clear() = apply { cleared = true }
        override fun apply() {
            if (cleared) values.clear()
            removed.forEach(values::remove)
            values.putAll(changes)
        }
        override fun commit(): Boolean = true.also { apply() }

        override fun putStringSet(key: String, values: Set<String>?) = throw NotImplementedError()
        override fun putInt(key: String, value: Int) = throw NotImplementedError()
        override fun putLong(key: String, value: Long) = throw NotImplementedError()
        override fun putFloat(key: String, value: Float) = throw NotImplementedError()
        override fun putBoolean(key: String, value: Boolean) = throw NotImplementedError()
    }
}
