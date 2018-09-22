package com.github.davinkevin.podcastserver.service

import arrow.core.Failure
import arrow.core.Try
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Supplier
import java.util.stream.Collector

@Service
class HtmlService(var urlService: UrlService) {

    val log = LoggerFactory.getLogger(this.javaClass.name)!!

    fun get(url: String): io.vavr.control.Option<Document> {

        val r = Try { urlService.get(url).header(UrlService.USER_AGENT_KEY, UrlService.USER_AGENT_DESKTOP).asBinary() }
                .filter { it.status < 400 }
                .map { it.body }
                .flatMap { Try { Jsoup.parse(it, StandardCharsets.UTF_8.name(), "") } }

        if (r is Failure) {
            log.error("Error during HTML Fetching of {}", url, r.exception)
        }

        return r.toOption().toVΛVΓ()
    }

    fun parse(html: String): Document = Jsoup.parse(html)

    companion object {

        @JvmStatic
        fun toElements(): Collector<Element, Elements, Elements> {
            return Collector.of<Element, Elements>(
                    Supplier { Elements() },
                    BiConsumer { obj, e -> obj.add(e) },
                    BinaryOperator { left, right -> left.addAll(right); left },
                    Collector.Characteristics.UNORDERED
            )
        }
    }
}