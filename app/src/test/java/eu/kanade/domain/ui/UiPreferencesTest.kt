package eu.kanade.domain.ui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class UiPreferencesTest {

    private val date = LocalDate.of(2026, 3, 14)

    @ParameterizedTest
    @ValueSource(strings = ["'", "{", "HH:mm"])
    fun `An invalid date format falls back to the default`(format: String) {
        UiPreferences.dateFormat(format, Locale.ENGLISH).format(date) shouldBe
            UiPreferences.dateFormat("", Locale.ENGLISH).format(date)
    }

    @ParameterizedTest
    @ValueSource(strings = ["dd/MM/yy", "yyyy-MM-dd"])
    fun `A valid date format is kept`(format: String) {
        UiPreferences.dateFormat(format, Locale.ENGLISH).format(date) shouldBe
            DateTimeFormatter.ofPattern(format, Locale.ENGLISH).format(date)
    }
}
