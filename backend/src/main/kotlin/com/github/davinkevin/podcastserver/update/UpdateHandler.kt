package com.github.davinkevin.podcastserver.update

import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import java.util.*

class UpdateHandler(
        private val update: UpdateService
) {

    fun updateAll(r: ServerRequest): ServerResponse {
        val force = r.paramOrNull("force")?.toBoolean() ?: false
        val withDownload = r.paramOrNull("download")?.toBoolean() ?: false

        update.updateAll(force, withDownload)

        return ServerResponse.ok().build()
    }

    fun update(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("podcastId")
            .let(UUID::fromString)

        update.update(id)

        return ServerResponse.ok().build()
    }
}
