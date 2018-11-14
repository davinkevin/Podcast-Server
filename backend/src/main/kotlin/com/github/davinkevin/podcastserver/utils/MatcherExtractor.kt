package com.github.davinkevin.podcastserver.utils

import io.vavr.API.None
import io.vavr.API.Option
import io.vavr.control.Option
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by kevin on 09/07/2018
 */
class MatcherExtractor(private val matcher: Matcher, private val isFind: Boolean) {

    constructor(m: Matcher) : this(m, m.find())

    companion object {
        @JvmStatic fun from(p: Pattern) = PatternExtractor(p)
        fun from(s: String) = PatternExtractor(s.toPattern())
    }

    fun group(i: Int): Option<String> = when {
        !isFind -> None()
        else -> Option(matcher.group(i))
    }

    fun groups(): Option<List<String>> = when {
        !isFind -> None()
        else -> Option((1..matcher.groupCount()).asSequence().map { matcher.group(it) }.toList())
    }

    class PatternExtractor(private val p: Pattern) {
        fun on(v: String) = MatcherExtractor(p.matcher(v))
    }
}