package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.control.Option
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.springframework.stereotype.Service

@Service
class JdomService (val urlService: UrlService){

    fun parse(url: String): Option<Document> {

        val v = Try { urlService.asStream(url).use { SAXBuilder().build(it) } }
                .getOrElse { throw RuntimeException("Error during parsing of $url") }

        return v.toOption().toVΛVΓ()
    }
}
