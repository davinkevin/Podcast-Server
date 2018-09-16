package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import arrow.core.getOrElse
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service

@Service
class SignatureService(val urlService: UrlService) {

    val log = getLogger(this.javaClass.name)!!

    fun fromUrl(url: String): MD5 {
        return Try {
            urlService.asStream(url).use { DigestUtils.md5Hex(it) }
        }
        .getOrElse {
            log.error("Error during signature of podcast at url {}", url, it)
            ""
        }
    }

    fun fromText(html: String) : MD5 = DigestUtils.md5Hex(html)!!
}

typealias MD5 = String