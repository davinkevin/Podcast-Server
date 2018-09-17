package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import arrow.core.getOrDefault
import org.springframework.stereotype.Service

/**
 * Created by kevin on 25/01/2016 for Podcast Server
 */
@Service
class ProcessService {

    fun newProcessBuilder(vararg command: String) = ProcessBuilder(*command)

    fun start(processBuilder: ProcessBuilder): Process = processBuilder.start()

    fun pidOf(p: Process) =
            Try { p.javaClass.simpleName }
                    .filter { className -> className.contains("UNIXProcess") }
                    .map { p.javaClass.getDeclaredField("pid") }
                    .map { it.apply { isAccessible = true } }
                    .map { it.getInt(p) }
                    .getOrDefault { -1 }

    fun waitFor(process: Process): Try<Int> {
        return Try { process.waitFor() }
    }
}