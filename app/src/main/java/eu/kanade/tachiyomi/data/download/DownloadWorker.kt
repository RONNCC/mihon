package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mihon.app.di.AppGraph
import mihon.core.metro.metroGraph
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.R
import kotlin.time.Duration.Companion.seconds

/**
 * This worker owns the lifecycle of the downloader: it starts the downloader and stops it when the
 * worker is stopped by the system or there's no suitable network available.
 */
class DownloadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val graph: AppGraph = context.metroGraph()

    @Inject private lateinit var downloader: Downloader

    @Inject private lateinit var downloadPreferences: DownloadPreferences

    init {
        graph.inject(this)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = applicationContext.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setContentTitle(applicationContext.getString(R.string.download_notifier_downloader_title))
            setSmallIcon(android.R.drawable.stat_sys_download)
        }.build()
        return ForegroundInfo(
            Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    override suspend fun doWork(): Result {
        downloader.awaitRestore()
        // Pausing cancels the worker, so one that runs was asked to download
        downloader.isPaused = false

        val startIssue = networkIssue()
        if (startIssue != null) {
            if (inputData.getBoolean(KEY_RESUME, false)) {
                // A resume starts as soon as its network constraint is met, which can be a moment before the
                // connection validates. Unmetered isn't Wi-Fi, so waiting on that mismatch would never end.
                return if (startIssue == NetworkIssue.NotWifi || downloader.queueState.value.isEmpty()) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }

            downloader.stop(applicationContext.getString(startIssue.message))
            resumeWhenOnline()
            return Result.success()
        }

        if (inputData.getBoolean(KEY_RESUME, false)) {
            // This worker only waited for the network. The one that downloads has no constraint, as WorkManager
            // would stop it on any drop before the grace period of watchNetwork could apply.
            if (downloader.queueState.value.isEmpty()) return Result.success()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>().addTag(TAG).build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            return Result.success()
        }

        if (!downloader.start()) return Result.failure()

        var lostNetwork: NetworkIssue? = null
        try {
            setForegroundSafely()

            lostNetwork = watchNetwork(
                isRunning = { downloader.isRunning },
                currentIssue = { networkIssue() },
                suspendDownloads = { downloader.suspendDownloads() },
                resumeDownloads = { downloader.resume() },
            )
        } finally {
            // Stopped by WorkManager means already canceled, and taking the lock would then throw instead of stopping
            if (downloader.isRunning && isStopped) {
                withContext(NonCancellable) { downloader.stop() }
            }
        }

        lostNetwork?.let { downloader.stop(applicationContext.getString(it.message)) }
        // Offline, the chapters can fail before the first check notices, so the connection is checked again
        if (lostNetwork != null || networkIssue() != null) resumeWhenOnline()
        return Result.success()
    }

    private fun networkIssue(): NetworkIssue? {
        val state = applicationContext.activeNetworkState()
        return when {
            !state.isOnline -> NetworkIssue.Offline
            downloadPreferences.downloadOnlyOverWifi.get() && !state.isWifi -> NetworkIssue.NotWifi
            else -> null
        }
    }

    /**
     * Queues a worker that continues the downloads once the network is back, unless the user paused them. It's
     * appended, as replacing this worker would cancel it before it finishes.
     */
    private fun resumeWhenOnline() {
        if (downloader.queueState.value.isEmpty() || downloader.isPaused) return

        val networkType = if (downloadPreferences.downloadOnlyOverWifi.get()) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(TAG)
            .setConstraints(Constraints(requiredNetworkType = networkType))
            .setInputData(workDataOf(KEY_RESUME to true))
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        private const val TAG = "Downloader"
        private const val KEY_RESUME = "resume"

        fun start(context: Context) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Cancels the running downloader worker and the one waiting for the network.
         */
        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }

        fun isRunningFlow(context: Context): Flow<Boolean> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(TAG)
                .asFlow()
                .map { list -> list.count { it.state == WorkInfo.State.RUNNING } == 1 }
        }
    }
}

/**
 * What keeps the downloader from running. A connection that drops for a few seconds, as it does when switching
 * between Wi-Fi and mobile data, is waited out for [graceSeconds] before the user is told.
 */
internal enum class NetworkIssue(@StringRes val message: Int, val graceSeconds: Int) {
    Offline(R.string.download_notifier_no_network, 10),
    NotWifi(R.string.download_notifier_text_only_wifi, 0),
}

/**
 * Checks once a second for a network issue while the downloader runs. At the first one the downloads are
 * suspended, so the chapters don't fail one after another while offline, and they're resumed if the network is
 * back within the issue's grace period.
 *
 * @return the issue that lasted past its grace period, or null once the downloader stopped.
 */
internal suspend fun watchNetwork(
    isRunning: () -> Boolean,
    currentIssue: () -> NetworkIssue?,
    suspendDownloads: suspend () -> Unit,
    resumeDownloads: suspend () -> Unit,
): NetworkIssue? {
    var consecutiveSeconds = 0
    var isSuspended = false
    while (isSuspended || isRunning()) {
        delay(1.seconds)
        val issue = currentIssue()
        if (issue == null) {
            consecutiveSeconds = 0
            if (isSuspended) {
                isSuspended = false
                resumeDownloads()
            }
            continue
        }

        consecutiveSeconds++
        if (!isSuspended) {
            isSuspended = true
            suspendDownloads()
        }
        if (consecutiveSeconds > issue.graceSeconds) return issue
    }
    return null
}
