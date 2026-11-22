package tachiyomi.domain.release.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

class GetApplicationReleaseTest {

    private lateinit var getApplicationRelease: GetApplicationRelease
    private lateinit var releaseService: ReleaseService

    @BeforeEach
    fun beforeEach() {
        releaseService = mockk()

        getApplicationRelease = GetApplicationRelease(releaseService)
    }

    @Test
    fun `When has update but is nightly expect new update`() = runTest {
        val release = Release(
            "r2000",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = true,
                commitCount = 1000,
                versionName = "",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `When has update expect new update`() = runTest {
        val release = Release(
            "v2.0.0",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `When has no update expect no new update`() = runTest {
        val release = Release(
            "v1.0.0",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = "v2.0.0",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun `Release version is compared component by component`() = runTest {
        isUpdateAvailable(tag = "v0.9.0", current = "1.0.0") shouldBe false
        isUpdateAvailable(tag = "v1.0.0", current = "0.9.0") shouldBe true
        isUpdateAvailable(tag = "v0.10.0", current = "0.9.5") shouldBe true
        isUpdateAvailable(tag = "v0.21.4", current = "0.21.3") shouldBe true
        isUpdateAvailable(tag = "v0.21.3", current = "0.21.3") shouldBe false
    }

    @Test
    fun `Release versions of different length are padded with zeros`() = runTest {
        isUpdateAvailable(tag = "v1.0", current = "1.0.0") shouldBe false
        isUpdateAvailable(tag = "v1.1", current = "1.0.5") shouldBe true
        isUpdateAvailable(tag = "v1.0.0.1", current = "1.0") shouldBe true
    }

    @Test
    fun `Build suffix of the installed version is ignored`() = runTest {
        isUpdateAvailable(tag = "v0.21.3", current = "0.21.3-1234") shouldBe false
        isUpdateAvailable(tag = "v0.21.4", current = "0.21.3-1234") shouldBe true
    }

    @Test
    fun `Fork release with a higher ronncc number is an update`() = runTest {
        isUpdateAvailable(tag = "v0.21.3-ronncc.5", current = "0.21.3-ronncc.4") shouldBe true
        isUpdateAvailable(tag = "v0.21.3-ronncc.4", current = "0.21.3-ronncc.5") shouldBe false
        isUpdateAvailable(tag = "v0.21.3-ronncc.5", current = "0.21.3-ronncc.5") shouldBe false
    }

    @Test
    fun `Fork release numbers are compared as numbers`() = runTest {
        isUpdateAvailable(tag = "v0.21.3-ronncc.10", current = "0.21.3-ronncc.9") shouldBe true
        isUpdateAvailable(tag = "v0.21.3-ronncc.9", current = "0.21.3-ronncc.10") shouldBe false
    }

    @Test
    fun `Upstream version of a fork release is compared before the ronncc number`() = runTest {
        isUpdateAvailable(tag = "v0.21.4-ronncc.1", current = "0.21.3-ronncc.9") shouldBe true
        isUpdateAvailable(tag = "v0.21.3-ronncc.9", current = "0.21.4-ronncc.1") shouldBe false
    }

    @Test
    fun `Plain upstream tag is never newer than a fork build`() = runTest {
        isUpdateAvailable(tag = "v0.21.3", current = "0.21.3-ronncc.5") shouldBe false
        isUpdateAvailable(tag = "v0.21.2", current = "0.21.3-ronncc.1") shouldBe false
        isUpdateAvailable(tag = "v0.22.0", current = "0.21.3-ronncc.5") shouldBe false
    }

    @Test
    fun `Fork release is newer than the plain upstream version it is based on`() = runTest {
        isUpdateAvailable(tag = "v0.21.3-ronncc.1", current = "0.21.3") shouldBe true
        isUpdateAvailable(tag = "v0.21.3-ronncc.1", current = "0.21.4") shouldBe false
    }

    @Test
    fun `Build suffix of an installed fork version is ignored`() = runTest {
        isUpdateAvailable(tag = "v0.21.3-ronncc.5", current = "0.21.3-ronncc.5-1234") shouldBe false
        isUpdateAvailable(tag = "v0.21.3-ronncc.6", current = "0.21.3-ronncc.5-1234") shouldBe true
    }

    @Test
    fun `Nightly check still compares the commit count only`() = runTest {
        coEvery { releaseService.latest(any()) } returns Release(
            "r2000",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )
        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = true,
                commitCount = 2000,
                versionName = "0.21.3-ronncc.5-2000",
                repository = "test",
            ),
        )
        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    private suspend fun isUpdateAvailable(tag: String, current: String): Boolean {
        coEvery { releaseService.latest(any()) } returns Release(
            tag,
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = current,
                repository = "test",
            ),
        )
        return result is GetApplicationRelease.Result.NewUpdate
    }
}
