package com.github.davinkevin.podcastserver.utils

import arrow.core.Option
import java.util.*

/**
 * Created by kevin on 11/08/2018
 */
fun <T> Optional<T>.k(): Option<T> = this.map { Option.just(it) }.orElse(Option.empty())
fun <T> io.vavr.control.Option<T>.k(): Option<T> = this.map { Option.just(it) }.getOrElse(Option.empty())