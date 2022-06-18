package com.github.davinkevin.podcastserver.extension.java.net

import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.extension

/**
 * Created by kevin on 12/07/2020
 */
fun URI.extension(): String = Path(this.path).extension
