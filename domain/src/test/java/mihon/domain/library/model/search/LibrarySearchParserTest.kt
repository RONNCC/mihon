package mihon.domain.library.model.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibrarySearchParserTest {

    @Test
    fun `Stray closing parenthesis does not drop the rest of the query`() {
        QueryNode.from("one ) two") shouldBe AndNode(
            listOf(GeneralQueryNode("one", false), GeneralQueryNode("two", false)),
        )
    }

    @Test
    fun `Stray closing parenthesis before an or keeps both sides`() {
        QueryNode.from("one ) || two") shouldBe OrNode(
            listOf(GeneralQueryNode("one", false), GeneralQueryNode("two", false)),
        )
    }

    @Test
    fun `Parenthesized group still ends at its closing parenthesis`() {
        QueryNode.from("(one || two) three") shouldBe AndNode(
            listOf(
                OrNode(listOf(GeneralQueryNode("one", false), GeneralQueryNode("two", false))),
                GeneralQueryNode("three", false),
            ),
        )
    }
}
