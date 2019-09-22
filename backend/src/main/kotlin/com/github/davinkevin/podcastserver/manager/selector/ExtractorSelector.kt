package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.Extractor
import org.springframework.aop.TargetClassAware
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.net.URI

/**
 * Created by kevin on 03/12/2017
 */
@Service
class ExtractorSelector(val context: ApplicationContext, val extractors: Set<Extractor>) {

    @Suppress("UNCHECKED_CAST")
    fun of(url: URI): Extractor{
        val e = extractors.minBy { it.compatibility(url) }
        val eClass = (if (e is TargetClassAware) e.targetClass else e?.javaClass) as Class<Extractor>
        return context.getBean(eClass)
    }
}
