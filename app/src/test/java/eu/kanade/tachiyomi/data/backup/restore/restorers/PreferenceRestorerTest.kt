package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.create.BackupCreateWorker
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.library.LibraryUpdateWorker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.PreferenceStore

class PreferenceRestorerTest {

    private val preferenceStore = mockk<PreferenceStore>(relaxed = true) {
        every { getAll() } returns emptyMap<String, Any>()
    }
    private val restorer = PreferenceRestorer(mockk(), mockk(), preferenceStore)

    @BeforeEach
    fun setUp() {
        mockkObject(LibraryUpdateWorker.Companion, BackupCreateWorker.Companion)
        every { LibraryUpdateWorker.setupTask(any(), any()) } just runs
        every { BackupCreateWorker.setupTask(any(), any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `App state in a backup isn't restored`() = runTest {
        restorer.restoreApp(
            listOf(
                BackupPreference("__APP_STATE_trusted_extensions", StringSetPreferenceValue(setOf("pkg:1:key"))),
                BackupPreference("__APP_STATE_storage_dir", StringPreferenceValue("content://elsewhere")),
                BackupPreference("app_date_format", StringPreferenceValue("dd/MM/yy")),
            ),
            backupCategories = null,
        )

        verify(exactly = 0) { preferenceStore.getStringSet("__APP_STATE_trusted_extensions", any()) }
        verify(exactly = 0) { preferenceStore.getString("__APP_STATE_storage_dir", any<String>()) }
        verify { preferenceStore.getString("app_date_format", any<String>()) }
    }
}
