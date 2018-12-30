package com.github.davinkevin.podcastserver.manager.selector

import lan.dk.podcastserver.manager.worker.Extractor
import lan.dk.podcastserver.manager.worker.noop.NoOpExtractor
import org.springframework.aop.TargetClassAware
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * Created by kevin on 03/12/2017
 */
@Service
class ExtractorSelector(val context: ApplicationContext, val extractors: Set<Extractor>) {

    @Suppress("UNCHECKED_CAST")
    fun of(url: String?) =
            if (url.isNullOrEmpty()) {
                NO_OP_EXTRACTOR
            } else {
                val e = extractors.minBy { it.compatibility(url) }!!
                val eClass = (if (e is TargetClassAware) e.targetClass else e.javaClass) as Class<Extractor>
                context.getBean(eClass)
            }

    companion object {
        val NO_OP_EXTRACTOR: Extractor = NoOpExtractor()
    }

}
