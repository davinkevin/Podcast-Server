package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import org.springframework.stereotype.Service

/**
 * Created by kevin on 25/01/2016 for Podcast Server
 */
@Service
class ProcessService {

    fun newProcessBuilder(vararg command: String) = ProcessBuilder(*command)

    fun start(processBuilder: ProcessBuilder): Process = processBuilder.start()

    fun pidOf(p: Process) = p.pid()

    fun waitFor(process: Process): Try<Int> = Try { process.waitFor() }
}
