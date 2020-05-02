package com.github.davinkevin.podcastserver.messaging

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Created by kevin on 02/05/2020
 */
@ConstructorBinding
@ConfigurationProperties("podcastserver.cluster")
class ClusterProperties(
        /**
         * Local IP of the current instance of the podcast-server
         */
        val local: String = "",
        /**
         * DNS entry to resolve to find all IPs of other podcast-server components
         */
        val dns: String = ""
)
