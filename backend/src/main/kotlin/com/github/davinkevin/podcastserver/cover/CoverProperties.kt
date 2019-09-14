package com.github.davinkevin.podcastserver.cover

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("podcastserver.cover")
class CoverProperties {
    var numberDayToKeep = 365
}
