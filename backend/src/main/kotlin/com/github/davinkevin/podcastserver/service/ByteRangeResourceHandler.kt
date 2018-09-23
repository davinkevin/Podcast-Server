package com.github.davinkevin.podcastserver.service

import arrow.core.Option
import arrow.core.getOrElse
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import java.nio.file.Path
import javax.servlet.http.HttpServletRequest

/**
 * Created by kevin on 01/09/2017
 */
@Component
class ByteRangeResourceHandler : ResourceHttpRequestHandler() {

    public override fun getResource(request: HttpServletRequest): Resource =
            Option.just(request.getAttribute(ATTR_FILE))
                    .filter { it is Path }
                    .map { it as Path }
                    .map { it.toFile() }
                    .map { FileSystemResource(it) }
                    .getOrElse { throw RuntimeException("Error during serving of byte range resources") }

    companion object {
        val ATTR_FILE = ByteRangeResourceHandler::class.java.name + ".file"
    }
}
