package com.github.davinkevin.podcastserver.update.updaters

import java.net.URI

/**
 * Created by kevin on 06/03/15.
 */
class UpdaterSelector(val updaters: Set<Updater>) {
    fun of(url: URI): Updater = updaters.minByOrNull { updater: Updater -> updater.compatibility(url.toASCIIString()) }!!
    fun types(): Set<Type> = updaters.map { it.type() }.toSet()
}
