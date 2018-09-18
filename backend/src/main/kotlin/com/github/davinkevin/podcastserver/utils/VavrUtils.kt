package com.github.davinkevin.podcastserver.utils

import arrow.core.None
import arrow.core.Some
import io.vavr.API

/**
 * Created by kevin on 20/07/2018
 */

fun <T> arrow.core.Option<T>.toVΛVΓ(): io.vavr.control.Option<T> {
    return when(this) {
        is Some -> API.Option(this.get())
        is None -> API.None()
    }
}