package eu.kanade.tachiyomi.util.storage

import android.content.ContentResolver
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class UriPermissionsTest {

    private val now = 100.days.inWholeMilliseconds

    private val treeIds = mutableMapOf<Uri, String>()

    private val resolver = mockk<ContentResolver>(relaxed = true)

    private val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    @BeforeEach
    fun setUp() {
        mockkStatic(DocumentsContract::class)
        every { DocumentsContract.isTreeUri(any()) } answers { firstArg<Uri>() in treeIds }
        every { DocumentsContract.getTreeDocumentId(any()) } answers { treeIds.getValue(firstArg()) }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DocumentsContract::class)
    }

    @Test
    fun `folders other than the storage folder are released`() {
        val storage = uri(treeId = "primary:Mihon")
        val storageAsDocument = uri(treeId = "primary:Mihon")
        val replaced = uri(treeId = "primary:Old")
        grants(storage to now, replaced to now)

        resolver.releaseStalePersistedPermissions(storageAsDocument, now)

        verify(exactly = 1) { resolver.releasePersistableUriPermission(replaced, flags) }
        verify(exactly = 0) { resolver.releasePersistableUriPermission(storage, any()) }
    }

    @Test
    fun `files are released a day after they were persisted`() {
        val stale = uri()
        val inUse = uri()
        grants(stale to now - 25.hours.inWholeMilliseconds, inUse to now - 1.hours.inWholeMilliseconds)

        resolver.releaseStalePersistedPermissions(null, now)

        verify(exactly = 1) { resolver.releasePersistableUriPermission(stale, flags) }
        verify(exactly = 0) { resolver.releasePersistableUriPermission(inUse, any()) }
    }

    @Test
    fun `folders are kept when the storage folder is not one`() {
        val folder = uri(treeId = "primary:Mihon")
        grants(folder to now)

        resolver.releaseStalePersistedPermissions(uri(), now)

        verify(exactly = 0) { resolver.releasePersistableUriPermission(any(), any()) }
    }

    private fun uri(treeId: String? = null): Uri {
        return mockk<Uri> { every { authority } returns "com.android.externalstorage.documents" }
            .also { if (treeId != null) treeIds[it] = treeId }
    }

    private fun grants(vararg grants: Pair<Uri, Long>) {
        every { resolver.persistedUriPermissions } returns grants.map { (grantedUri, persistedAt) ->
            mockk<UriPermission> {
                every { uri } returns grantedUri
                every { persistedTime } returns persistedAt
                every { isReadPermission } returns true
                every { isWritePermission } returns true
            }
        }
    }
}
