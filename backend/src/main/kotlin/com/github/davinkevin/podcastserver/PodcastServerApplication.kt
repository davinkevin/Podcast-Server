package com.github.davinkevin.podcastserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Created by kevin on 2019-02-09
 */
@SpringBootApplication
class PodcastServerApplication

fun main(args: Array<String>) {
    System.getProperties().setProperty("org.jooq.no-logo", "true")
    runApplication<PodcastServerApplication>(*args)
}
