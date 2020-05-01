package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import java.net.URI

/**
 * Created by kevin on 06/03/15.
 */
class UpdaterSelector(val updaters: Set<Updater>) {

    fun of(url: URI): Updater = updaters.minBy { updater -> updater.compatibility(url.toASCIIString()) }!!
    fun types(): Set<Type> = updaters.map { it.type() }.toSet()
}
