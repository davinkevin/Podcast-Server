package com.github.davinkevin.podcastserver.extension.java.net

import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.extension

/**
 * Created by kevin on 12/07/2020
 */
fun URI.extension(): String = Path(this.path).extension

/**
 * Extension under which a cover downloaded from this URI is stored in the bucket.
 * Falls back to `jpg` when the URI path carries no extension.
 */
fun URI.coverExtension(): String = extension().ifBlank { "jpg" }
