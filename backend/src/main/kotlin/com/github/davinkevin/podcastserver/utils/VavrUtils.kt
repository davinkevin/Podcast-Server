package com.github.davinkevin.podcastserver.utils

import arrow.core.None
import arrow.core.Some
import io.vavr.API
import io.vavr.collection.HashSet
import io.vavr.collection.List

/**
 * Created by kevin on 20/07/2018
 */

fun <T> arrow.core.Option<T>.toVΛVΓ(): io.vavr.control.Option<T> {
    return when(this) {
        is Some -> API.Option(this.get())
        is None -> API.None()
    }
}

fun <T> Set<T>.toVΛVΓ(): io.vavr.collection.Set<T> = HashSet.ofAll(this)
fun <T> kotlin.collections.List<T>.toVΛVΓ(): io.vavr.collection.List<T> = List.ofAll(this)