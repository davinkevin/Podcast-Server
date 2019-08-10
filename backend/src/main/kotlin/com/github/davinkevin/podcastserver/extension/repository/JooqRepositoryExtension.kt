package com.github.davinkevin.podcastserver.extension.repository

import org.jooq.InsertResultStep
import org.jooq.Query
import org.jooq.Record
import org.jooq.Result
import org.jooq.ResultQuery
import org.jooq.UpdateResultStep
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.util.concurrent.CompletionStage

/**
 * Created by kevin on 2019-02-12
 */

private val log = LoggerFactory.getLogger("JooqExtension")!!

fun <R: Record> ResultQuery<R>.fetchAsFlux(): Flux<R> = Flux.defer {
    Mono.fromCompletionStage(this.fetchAsync()).flatMapMany { Flux.fromIterable(it) }
}

fun <R: Record> ResultQuery<R>.fetchOneAsMono(): Mono<R> = Mono.defer {
    Mono.fromCompletionStage(this.fetchAsync())
            .flatMapMany { Flux.fromIterable(it) }
            .toMono()
}

private fun <R: Record> CompletionStage<Result<R>>.toFlux(): Flux<R> =
        Mono.fromCompletionStage(this).flatMapMany { Flux.fromIterable(it) }

fun Query.executeAsyncAsMono(): Mono<Int> = Mono.defer { Mono.fromCompletionStage(executeAsync()) }

fun <R: Record> InsertResultStep<R>.fetchOneAsMono(): Mono<R> =
        Mono.justOrEmpty(this.fetchOptional())

fun <R: Record> UpdateResultStep<R>.fetchOneAsMono(): Mono<R> =
        Mono.justOrEmpty(this.fetchOptional())

fun <R: Record> UpdateResultStep<R>.fetchAsFlux(): Flux<R> =
        Mono.justOrEmpty(this.fetch()).flatMapMany { it.toFlux() }
