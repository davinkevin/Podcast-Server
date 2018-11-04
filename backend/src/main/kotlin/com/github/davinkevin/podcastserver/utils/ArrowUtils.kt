package com.github.davinkevin.podcastserver.utils

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import java.util.*

/**
 * Created by kevin on 11/08/2018
 */

fun <T> Optional<T>.k(): Option<T> =
        when {
            this.isPresent -> Some(this.get())
            else -> None
        }