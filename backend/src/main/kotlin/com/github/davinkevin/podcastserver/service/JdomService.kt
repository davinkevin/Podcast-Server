package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.WatchList
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.control.Option
import com.github.davinkevin.podcastserver.entity.Item
import org.apache.commons.io.FilenameUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Boolean.TRUE
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Objects.nonNull
import java.util.function.Function

@Service
class JdomService (val urlService: UrlService){

    fun parse(url: String): Option<Document> {

        val v = Try { urlService.asStream(url).use { SAXBuilder().build(it) } }
                .getOrElse { throw RuntimeException("Error during parsing of $url") }

        return v.toOption().toVΛVΓ()
    }
}
