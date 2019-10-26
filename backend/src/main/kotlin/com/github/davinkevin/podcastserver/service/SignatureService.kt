package com.github.davinkevin.podcastserver.service

import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service

@Service
class SignatureService(val urlService: UrlService) {

    private val log = getLogger(SignatureService::class.java)

    fun fromUrl(url: String): MD5 {
        return try {
            urlService.asStream(url).use { DigestUtils.md5Hex(it) }
        } catch (e: Exception) {
            log.error("Error during signature of podcast at url {}", url, e)
            ""
        }
    }

    fun fromText(html: String) : MD5 = DigestUtils.md5Hex(html)!!
}

typealias MD5 = String
