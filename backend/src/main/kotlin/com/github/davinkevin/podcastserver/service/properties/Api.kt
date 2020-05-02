package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@ConstructorBinding
@ConfigurationProperties("podcastserver.api")
data class Api(val youtube: String = "")
