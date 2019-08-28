package com.github.davinkevin.podcastserver.extension.reactor

import io.vavr.control.Option

fun <T> Option<T>.toMono() = reactor.core.publisher.Mono.justOrEmpty(toJavaOptional())
