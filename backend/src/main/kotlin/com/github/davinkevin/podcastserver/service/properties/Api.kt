package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@ConfigurationProperties("podcastserver.api")
class Api(var youtube: String? = null, var dailymotion: String? = null)
