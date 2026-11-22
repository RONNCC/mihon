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
            // Release builds: based on releases in "mihonapp/mihon" repo
            // tagged as something like "v0.1.2"
            compareVersions(parseVersion(versionTag), parseVersion(versionName)) > 0
        }
    }

    /** Leading numeric components of a version like "v0.21.3" or "0.21.3-1234", ignoring any suffix. */
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
    }
}
