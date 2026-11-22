package eu.kanade.tachiyomi.util.storage

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration.Companion.days

/**
 * Releases the persisted permissions the app has no use for anymore.
 *
 * Every backup created or restored, and every storage folder replaced, leaves one behind, and the system only keeps
 * a limited number by dropping the oldest, which could end up being the storage folder's.
 *
 * Folders other than [activeStorageDir] are released. Files are released after a day, so one a running backup or
 * restore still needs is kept. Folders are left alone when there's no [activeStorageDir] to tell them apart from.
 *
 * @param now the current time in milliseconds.
 */
fun ContentResolver.releaseStalePersistedPermissions(activeStorageDir: Uri?, now: Long = System.currentTimeMillis()) {
    val activeTree = activeStorageDir?.takeIf { DocumentsContract.isTreeUri(it) }?.let { it.authority to it.treeId() }
    for (permission in persistedUriPermissions) {
        val uri = permission.uri
        val isStale = if (DocumentsContract.isTreeUri(uri)) {
            activeTree != null && (uri.authority to uri.treeId()) != activeTree
        } else {
            now - permission.persistedTime > 1.days.inWholeMilliseconds
        }
        if (!isStale) continue

        var flags = 0
        if (permission.isReadPermission) flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (permission.isWritePermission) flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            releasePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            logcat(LogPriority.WARN, e) { "Could not release the permission to $uri" }
        }
    }
}

private fun Uri.treeId(): String = DocumentsContract.getTreeDocumentId(this)
