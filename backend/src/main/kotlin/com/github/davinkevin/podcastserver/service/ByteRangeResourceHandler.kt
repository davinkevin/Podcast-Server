package com.github.davinkevin.podcastserver.service

import org.springframework.stereotype.Component

/**
 * Created by kevin on 01/09/2017
 */
@Component
class ByteRangeResourceHandler /*: ResourceHttpRequestHandler*/() {

//    public override fun getResource(request: HttpServletRequest): Resource =
//            Option.just(request.getAttribute(ATTR_FILE))
//                    .filter { it is Path }
//                    .map { it as Path }
//                    .map { it.toFile() }
//                    .map { FileSystemResource(it) }
//                    .getOrElse { throw RuntimeException("Error during serving of byte range resources") }

    companion object {
        val ATTR_FILE = ByteRangeResourceHandler::class.java.name + ".file"
    }
}
