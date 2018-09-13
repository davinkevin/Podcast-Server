package com.github.davinkevin.podcastserver.utils

import io.vavr.API
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.regex.Pattern

internal class MatcherExtractorTest {

    @Test
    fun `should extract value`() {
        /* GIVEN */
        val s = "foo"
        val p = Pattern.compile("(.*)")

        /* WHEN  */
        val v = MatcherExtractor.from(p).on(s).group(1)

        /* THEN  */
        Assertions.assertThat(v).containsExactly("foo")
    }

    @Test
    fun `should not return any value`() {
        /* GIVEN */
        val s = ""
        val p = Pattern.compile("abc")

        /* WHEN  */
        val v = MatcherExtractor.from(p).on(s).group(1)

        /* THEN  */
        Assertions.assertThat(v).isEqualTo(API.None<Any>())
    }
}