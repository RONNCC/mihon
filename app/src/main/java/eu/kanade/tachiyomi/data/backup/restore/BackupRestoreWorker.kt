package eu.kanade.tachiyomi.data.backup.restore

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.app.di.AppGraph
import mihon.core.metro.metroGraph
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import java.io.File
import java.io.IOException

class BackupRestoreWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val graph: AppGraph = context.metroGraph()

    @Inject private lateinit var backupRestorerFactory: BackupRestorer.Factory

    @Inject private lateinit var notifier: BackupNotifier

    override suspend fun doWork(): Result {
        graph.inject(this)

        val uri = inputData.getString(LOCATION_URI_KEY)?.toUri()
        val options = inputData.getBooleanArray(OPTIONS_KEY)?.let { RestoreOptions.fromBooleanArray(it) }

        if (uri == null || options == null) {
            return Result.failure()
        }

        val isSync = inputData.getBoolean(SYNC_KEY, false)

        setForegroundSafely()

        // A backup opened from another app can only be read while the app's task lives, but the work is rerun after
        // the process dies, so it restores from its own copy
        val backupCopy = File(context.cacheDir, "restore_$id.tachibk")
        return try {
            withContext(Dispatchers.IO) {
                if (!backupCopy.exists()) copyBackup(uri, backupCopy)
                backupRestorerFactory.create(notifier = notifier, isSync = isSync)
                    .restore(backupCopy.toUri(), options)
            }
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) {
                notifier.showRestoreError(context.stringResource(MR.strings.restoring_backup_canceled))
                Result.success()
            } else {
                logcat(LogPriority.ERROR, e)
                notifier.showRestoreError(e.message)
                Result.failure()
            }
        } finally {
            backupCopy.delete()
            context.cancelNotification(Notifications.ID_RESTORE_PROGRESS)
        }
    }

    private fun copyBackup(uri: Uri, to: File) {
        val partial = File(to.path + ".part")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Can't open $uri")
            input.use { source -> partial.outputStream().use { source.copyTo(it) } }
            if (!partial.renameTo(to)) throw IOException("Can't copy the backup to $to")
        } finally {
            partial.delete()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_RESTORE_PROGRESS,
            notifier.showRestoreProgress().build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    companion object {
        suspend fun isRunning(workManager: WorkManager): Boolean {
            return workManager.isRunning(TAG)
        }

        fun start(
            workManager: WorkManager,
            uri: Uri,
            options: RestoreOptions,
            sync: Boolean = false,
        ) {
            val inputData = workDataOf(
                LOCATION_URI_KEY to uri.toString(),
                SYNC_KEY to sync,
                OPTIONS_KEY to options.asBooleanArray(),
            )
            val request = OneTimeWorkRequestBuilder<BackupRestoreWorker>()
                .addTag(TAG)
                .setInputData(inputData)
                .build()
            workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(workManager: WorkManager) {
            workManager.cancelUniqueWork(TAG)
        }
    }
}

private const val TAG = "BackupRestore"

private const val LOCATION_URI_KEY = "location_uri" // String
private const val SYNC_KEY = "sync" // Boolean
private const val OPTIONS_KEY = "options" // BooleanArray
