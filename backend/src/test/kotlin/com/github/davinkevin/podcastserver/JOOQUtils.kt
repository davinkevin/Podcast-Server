package com.github.davinkevin.podcastserver

import com.github.davinkevin.podcastserver.config.JooqConfig
import org.jooq.Batch
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.impl.DSL
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.lang.annotation.Inherited

/**
 * Created by kevin on 31/12/2021
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@ExtendWith(SpringExtension::class)
@OverrideAutoConfiguration(enabled = false)
@Import(JooqConfig::class)
@ImportAutoConfiguration(
    R2dbcAutoConfiguration::class,
    TransactionAutoConfiguration::class,
    R2dbcTransactionManagerAutoConfiguration::class
)
annotation class JooqR2DBCTest

fun <T: Record> ResultQuery<T>.r2dbc() = JooqReactorResultQueryWrapper(this)
class JooqReactorResultQueryWrapper<T: Record>(private val query: ResultQuery<T>) {

    fun fetchOne(): T = query.toMono().block()!!

    fun <T> fetchOne(field: Field<T>): T {
        return query.toMono()
            .map { it[field] }
            .block()!!
    }
    fun fetch(): List<T> {
        return Flux.from(query)
            .collectList()
            .block()!!
    }
}

fun Batch.r2dbc() = JooqReactorBatchWrapper(this)
class JooqReactorBatchWrapper(private val batch: Batch) {
    fun execute(): IntArray = batch.toFlux().collectList().block()!!.toIntArray()
}

