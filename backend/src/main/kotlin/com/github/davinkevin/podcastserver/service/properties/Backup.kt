package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@ConfigurationProperties("podcastserver.backup")
class Backup(
        var location: Path = Paths.get("/tmp"),
        var binary: Boolean = false,
        var cron: String = "0 0 4 * * *"
)