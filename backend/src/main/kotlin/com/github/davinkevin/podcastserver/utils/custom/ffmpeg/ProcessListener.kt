package com.github.davinkevin.podcastserver.utils.custom.ffmpeg

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit



class ProcessListener(val url: String) {

    companion object {
        val DEFAULT_PROCESS_LISTENER = ProcessListener("")
    }

    private val pool: ExecutorService = Executors.newFixedThreadPool(10)
    var process: Process? = null

    fun findProcess(): Future<Process> {
        return pool.submit<Process> {
            while (process === null) {
                TimeUnit.MILLISECONDS.sleep(100)
            }
            return@submit process
        }
    }
}