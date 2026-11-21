package mihon.core.archive

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import me.zhanghai.android.libarchive.ArchiveException
import java.io.Closeable
import java.io.InputStream

class ArchiveReader(pfd: ParcelFileDescriptor) : Closeable {
    private val size = pfd.statSize
    private val address = Os.mmap(0, size, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, pfd.fileDescriptor, 0)

    private val lock = Any()
    private var isClosed = false
    private var openStreams = 0

    fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T = openStream().use {
        block(generateSequence { it.getNextEntry() })
    }

    fun getInputStream(entryName: String): InputStream? {
        val archive = openStream()
        try {
            while (true) {
                val entry = archive.getNextEntry() ?: break
                if (entry.name == entryName) {
                    return archive
                }
            }
        } catch (e: ArchiveException) {
            archive.close()
            throw e
        }
        archive.close()
        return null
    }

    /**
     * The mapping outlives [close] until its streams are closed, and no stream opens after it: reading unmapped
     * memory crashes the process.
     */
    private fun openStream(): ArchiveInputStream = synchronized(lock) {
        check(!isClosed) { "Archive is closed" }
        openStreams++
        ArchiveInputStream(address, size, onClose = ::onStreamClosed)
    }

    private fun onStreamClosed() = synchronized(lock) {
        openStreams--
        unmapIfUnused()
    }

    override fun close() {
        synchronized(lock) {
            if (isClosed) return
            isClosed = true
            unmapIfUnused()
        }
    }

    private fun unmapIfUnused() {
        if (isClosed && openStreams == 0) {
            Os.munmap(address, size)
        }
    }
}
