package com.github.davinkevin.podcastserver.manager.worker.mycanal

import io.vavr.API.None
import io.vavr.API.Option
import io.vavr.control.Option
import org.apache.commons.lang3.StringUtils
import javax.validation.constraints.NotEmpty

/**
 * Created by kevin on 26/12/2017
 */
internal fun extractJsonConfig(text: String): Option<String> {
    val startToken = "__data="
    val endToken = "};"

    if (!text.contains(startToken) || !text.contains(endToken)) {
        // log.error("Structure of MyCanal page changed")
        return None()
    }

    val begin = text.indexOf(startToken)
    val end = text.indexOf(endToken, begin)
    return Option(text.substring(begin + startToken.length, end + 1))
}

fun myCanalCompatibility(@NotEmpty url: String?): Int {
    return if (StringUtils.contains(url, "www.mycanal.fr")) 1 else Integer.MAX_VALUE
}