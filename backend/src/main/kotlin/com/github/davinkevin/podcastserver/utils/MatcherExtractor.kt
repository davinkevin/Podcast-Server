package com.github.davinkevin.podcastserver.utils

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

    fun group(i: Int): String? = when {
        !isFind -> null
        else -> matcher.group(i)
    }

    class PatternExtractor(private val p: Pattern) {
        fun on(v: String) = MatcherExtractor(p.matcher(v))
    }
}
