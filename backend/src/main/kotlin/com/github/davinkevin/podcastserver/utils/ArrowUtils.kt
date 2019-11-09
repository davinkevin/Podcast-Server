package com.github.davinkevin.podcastserver.utils

import arrow.core.Option
import arrow.core.Try
import arrow.core.getOrElse
import java.util.*

/**
 * Created by kevin on 11/08/2018
 */
fun <T> Optional<T>.k(): Option<T> = this.map { Option.just(it) }.orElse(Option.empty())

fun <A> Option<A>.toTry() = Try { this.getOrElse { throw NoSuchElementException("No value present") } }
