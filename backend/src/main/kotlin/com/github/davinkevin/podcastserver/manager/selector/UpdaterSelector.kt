package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.manager.worker.noop.NoOpUpdater
import org.springframework.stereotype.Service

/**
 * Created by kevin on 06/03/15.
 */
@Service
class UpdaterSelector(val updaters: Set<Updater>) {

    fun of(url: String?): Updater =
            if(url.isNullOrEmpty()) {
                NO_OP_UPDATER
            } else {
                updaters.minBy { updater -> updater.compatibility(url) }!!
            }

    fun types() = updaters.map { it.type() }.toSet().toVΛVΓ()

    companion object {
        @JvmStatic
        val NO_OP_UPDATER = NoOpUpdater()
    }


}
