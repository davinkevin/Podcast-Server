package com.github.davinkevin.podcastserver.item

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemHandler(val itemService: ItemService) {

    fun clean(s: ServerRequest) =
            itemService
                    .deleteOldEpisodes()
                    .flatMap { ok().build() }
}