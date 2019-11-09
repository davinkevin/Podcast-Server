package com.github.davinkevin.podcastserver.manager.worker.mycanal

import org.apache.commons.lang3.StringUtils
import javax.validation.constraints.NotEmpty

/**
 * Created by kevin on 26/12/2017
 */
internal fun extractJsonConfig(text: String): String? {
    val startToken = "__data="
    val endToken = "};"

    if (!text.contains(startToken) || !text.contains(endToken)) {
        // log.error("Structure of MyCanal page changed")
        return null;
    }

    val begin = text.indexOf(startToken)
    val end = text.indexOf(endToken, begin)
    return text.substring(begin + startToken.length, end + 1)
}

fun myCanalCompatibility(@NotEmpty url: String?): Int {
    return if (StringUtils.contains(url, "www.mycanal.fr")) 1 else Integer.MAX_VALUE
}
