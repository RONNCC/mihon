package eu.kanade.tachiyomi.data.backup

import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.create.creators.CategoriesBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.ExtensionStoresBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.MangaBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SourcesBackupCreator
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream

class BackupCreatorTest {

    private val uri = mockk<Uri>()
    private val file = mockk<UniFile>(relaxed = true)
    private val mangaBackupCreator = mockk<MangaBackupCreator>()

    private val creator = BackupCreator(
        isAutoBackup = false,
        context = mockk(relaxed = true),
        parser = ProtoBuf,
        getFavorites = mockk { coEvery { await() } returns emptyList() },
        backupPreferences = mockk(relaxed = true),
        mangaRepository = mockk { coEvery { getReadMangaNotInLibrary() } returns emptyList() },
        categoriesBackupCreator = mockk<CategoriesBackupCreator>().also { coEvery { it() } returns emptyList() },
        mangaBackupCreator = mangaBackupCreator,
        preferenceBackupCreator = mockk {
            every { createApp(any()) } returns emptyList()
            coEvery { createSource(any()) } returns emptyList()
        },
        extensionStoresBackupCreator = mockk<ExtensionStoresBackupCreator>().also {
            coEvery { it() } returns emptyList()
        },
        sourcesBackupCreator = mockk<SourcesBackupCreator>().also { coEvery { it(any()) } returns emptyList() },
        backupFileValidator = mockk(relaxed = true),
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(UniFile::class)
        every { UniFile.fromUri(any(), uri) } returns file
        every { file.isDirectory } returns false
        every { file.isFile } returns false
        every { file.openOutputStream() } returns ByteArrayOutputStream()
        every { file.uri } returns mockk<Uri>(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UniFile::class)
    }

    private fun entries(entries: Flow<BackupManga>) {
        every { mangaBackupCreator(any(), any()) } returns entries
    }

    @Test
    fun `a created file the provider doesn't report as a file is written and kept`() = runTest {
        entries(flow { emit(BackupManga(source = 1L, url = "/manga")) })

        creator.backup(uri, BackupOptions())

        verify { file.openOutputStream() }
        verify(exactly = 0) { file.delete() }
    }

    @Test
    fun `running out of memory while writing deletes the partial file`() = runTest {
        entries(flow { throw OutOfMemoryError() })

        assertThrows<OutOfMemoryError> { creator.backup(uri, BackupOptions()) }

        verify(exactly = 1) { file.delete() }
    }

    @Test
    fun `a directory is not written to or deleted`() = runTest {
        every { file.isDirectory } returns true
        entries(flow { emit(BackupManga(source = 1L, url = "/manga")) })

        assertThrows<IllegalStateException> { creator.backup(uri, BackupOptions()) }

        verify(exactly = 0) { file.openOutputStream() }
        verify(exactly = 0) { file.delete() }
    }
}
