package com.github.davinkevin.podcastserver.extension.java.net

import java.net.URI

/**
 * Created by kevin on 12/07/2020
 */
fun URI.extension(): String {
    val path = this.path

    if(!path.contains(".")) {
        return "jpg"
    }

    return path.substringAfterLast(".")
}
