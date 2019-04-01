package com.github.davinkevin.podcastserver.extension.repository

import org.jooq.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.util.concurrent.CompletionStage

/**
 * Created by kevin on 2019-02-12
 */
fun <R: Record> ResultQuery<R>.fetchAsFlux(): Flux<R> =
        Mono.fromCompletionStage(this.fetchAsync()).flatMapMany { Flux.fromIterable(it) }

fun <R: Record> ResultQuery<R>.fetchOneAsMono(): Mono<R> =
        Mono.fromCompletionStage(this.fetchAsync())
                .flatMapMany { Flux.fromIterable(it) }
                .toMono()

private fun <R: Record> CompletionStage<Result<R>>.toFlux(): Flux<R> =
        Mono.fromCompletionStage(this).flatMapMany { Flux.fromIterable(it) }

fun Query.executeAsyncAsMono(): Mono<Int> = Mono.defer { Mono.fromCompletionStage(executeAsync()) }

fun <R: Record> InsertResultStep<R>.fetchOneAsMono(): Mono<R> =
        Mono.justOrEmpty(this.fetchOptional())

fun <R: Record> UpdateResultStep<R>.fetchOneAsMono(): Mono<R> =
        Mono.justOrEmpty(this.fetchOptional())

fun <R: Record> UpdateResultStep<R>.fetchAsFlux(): Flux<R> =
        Mono.justOrEmpty(this.fetch()).flatMapMany { it.toFlux() }
