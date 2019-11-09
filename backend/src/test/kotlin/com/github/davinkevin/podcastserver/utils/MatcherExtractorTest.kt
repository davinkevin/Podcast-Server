package com.github.davinkevin.podcastserver.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class MatcherExtractorTest {

    @Test
    fun `should extract value`() {
        /* GIVEN */
        val s = "foo"
        val p = Pattern.compile("(.*)")

        /* WHEN  */
        val v = MatcherExtractor.from(p).on(s).group(1)

        /* THEN  */
        assertThat(v).isEqualTo("foo")
    }

    @Test
    fun `should not return any value`() {
        /* GIVEN */
        val s = ""
        val p = Pattern.compile("abc")

        /* WHEN  */
        val v = MatcherExtractor.from(p).on(s).group(1)

        /* THEN  */
        assertThat(v).isNull()
    }
}
