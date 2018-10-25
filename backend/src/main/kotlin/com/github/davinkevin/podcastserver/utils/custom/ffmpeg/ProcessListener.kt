package com.github.davinkevin.podcastserver.utils.custom.ffmpeg

import java.util.concurrent.CompletableFuture


class ProcessListener(val url: String) {

    companion object {
        val DEFAULT_PROCESS_LISTENER = ProcessListener("")
    }

    var process: CompletableFuture<Process> = CompletableFuture()

    fun withProcess(p: Process): ProcessListener {
        process.complete(p)
        return this
    }
}