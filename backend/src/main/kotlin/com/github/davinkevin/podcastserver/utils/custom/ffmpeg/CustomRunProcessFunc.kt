package com.github.davinkevin.podcastserver.utils.custom.ffmpeg

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.ProcessListener.Companion.DEFAULT_PROCESS_LISTENER
import net.bramp.ffmpeg.RunProcessFunction
import java.io.IOException

/**
 * Created by kevin on 24/07/2016.
 */
class CustomRunProcessFunc(private var listeners: List<ProcessListener> = listOf()) : RunProcessFunction() {

    @Throws(IOException::class)
    override fun run(args: List<String>): Process {
        val p = super.run(args)

        val toBeRemoved = listeners
                .firstOption { pl -> args.contains(pl.url) }
                .map { it.withProcess(p) }
                .getOrElse { DEFAULT_PROCESS_LISTENER }

        this.listeners = listeners - toBeRemoved

        return p
    }

    fun add(pl: ProcessListener): CustomRunProcessFunc {
        this.listeners = listeners + pl
        return this
    }

    operator fun plus(pl: ProcessListener): CustomRunProcessFunc = this.add(pl)
}
