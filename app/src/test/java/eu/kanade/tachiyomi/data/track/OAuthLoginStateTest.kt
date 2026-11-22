package eu.kanade.tachiyomi.data.track

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.time.Duration.Companion.minutes

class OAuthLoginStateTest {

    private var now = 0L
    private val loginState = OAuthLoginState(InMemoryPreferenceStore().getString("state", "")) { now }

    @Test
    fun `A result without a pending login is rejected`() {
        loginState.finish("state") shouldBe false
        loginState.finish(null) shouldBe false
        loginState.finishWithoutState() shouldBe false
    }

    @Test
    fun `The state the login was started with is accepted once`() {
        val state = loginState.start()

        loginState.finish(state) shouldBe true
        loginState.finish(state) shouldBe false
    }

    @Test
    fun `A wrong or missing state is rejected and keeps the login pending`() {
        val state = loginState.start()

        loginState.finish("attacker") shouldBe false
        loginState.finish(null) shouldBe false
        loginState.finish(state) shouldBe true
    }

    @Test
    fun `Starting again replaces the pending state`() {
        val first = loginState.start()
        val second = loginState.start()

        second shouldNotBe first
        loginState.finish(first) shouldBe false
        loginState.finish(second) shouldBe true
    }

    @Test
    fun `An expired login is rejected`() {
        val state = loginState.start()
        now += 16.minutes.inWholeMilliseconds

        loginState.finish(state) shouldBe false
        now = 0
        loginState.finish(state) shouldBe false
    }

    @Test
    fun `A provider that doesn't send the state back needs a pending login`() {
        loginState.start()

        loginState.finishWithoutState() shouldBe true
        loginState.finishWithoutState() shouldBe false
    }
}
