package com.github.davinkevin.podcastserver.extension.java.net

import org.apache.commons.io.FilenameUtils
import java.net.URI

/**
 * Created by kevin on 12/07/2020
 */
fun URI.extension(): String = FilenameUtils
        .getExtension(this.toASCIIString())
        .let { if (it.isEmpty()) "jpg" else it.substringBeforeLast("?") }
