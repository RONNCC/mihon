package tachiyomi.domain.release.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

@Inject
class GetApplicationRelease(
    private val service: ReleaseService,
) {
    suspend fun await(arguments: Arguments): Result {
        val release = service.latest(arguments) ?: return Result.NoNewUpdate

        // Check if latest version is different from current version
        val isNewVersion = isNewVersion(
            arguments.isNightly,
            arguments.commitCount,
            arguments.versionName,
            release.version,
        )
        return when {
            isNewVersion -> Result.NewUpdate(release)
            else -> Result.NoNewUpdate
        }
    }

    private fun isNewVersion(
        isNightly: Boolean,
        commitCount: Int,
        versionName: String,
        versionTag: String,
    ): Boolean {
        return if (isNightly) {
            // Nightly builds: based on releases in "mihonapp/mihon-preview" repo
            // tagged as something like "r1234"
            versionTag.replace("[^\\d.]".toRegex(), "").toInt() > commitCount
        } else {
            // Release builds: based on releases in the fork's "RONNCC/mihon" repo
            // tagged as something like "v0.1.2" or, for the fork, "v0.1.2-ronncc.3"
            isNewRelease(versionTag, versionName)
        }
    }

    private fun isNewRelease(versionTag: String, versionName: String): Boolean {
        val tagFork = parseForkBuild(versionTag)
        val currentFork = parseForkBuild(versionName)
        // A fork build only follows the fork's own releases, so a plain upstream-style tag is never newer.
        if (currentFork != null && tagFork == null) return false

        val diff = compareVersions(parseVersion(versionTag), parseVersion(versionName))
        return diff > 0 || (diff == 0 && (tagFork ?: 0) > (currentFork ?: 0))
    }

    /** The N of a fork version like "v0.21.3-ronncc.5" or "0.21.3-ronncc.5-1234", or null if it isn't a fork one. */
    private fun parseForkBuild(version: String): Int? {
        return FORK_REGEX.find(version)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** Leading numeric components of a version like "v0.21.3" or "0.21.3-ronncc.5-1234", ignoring any suffix. */
    private fun parseVersion(version: String): List<Int> {
        return VERSION_REGEX.find(version)?.value.orEmpty()
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
    }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    data class Arguments(
        val isFoss: Boolean,
        val isNightly: Boolean,
        val commitCount: Int,
        val versionName: String,
        val repository: String,
        val forceCheck: Boolean = false,
    )

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
    }

    private companion object {
        val VERSION_REGEX = "\\d+(\\.\\d+)*".toRegex()
        val FORK_REGEX = "-ronncc\\.(\\d+)".toRegex()
    }
}
