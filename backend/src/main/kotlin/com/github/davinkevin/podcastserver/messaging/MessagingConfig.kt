package com.github.davinkevin.podcastserver.messaging

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.env.get
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.router


/**
 * Created by kevin on 01/05/2020
 */
@Configuration
@Import(MessageHandler::class)
class MessagingRoutingConfig {

    @Bean
    fun messageRouter(message: MessageHandler) = router {
        GET("/api/v1/sse", message::sseMessages)
        POST("/api/v1/sse/sync", message::sync)
    }

}

@Configuration
@Import(
        MessagingRoutingConfig::class,
        MessagingTemplate::class
)
@EnableConfigurationProperties(ClusterProperties::class, ServerProperties::class)
class MessagingConfig {

    @Bean
    @Conditional(OnClusterModeCondition::class)
    fun messageSyncClient(
            message: MessagingTemplate,
            wcb: WebClient.Builder,
            cluster: ClusterProperties,
            server: ServerProperties
    ) = MessageSyncClient(message, DNSClient(), wcb, cluster, server)
}

class OnClusterModeCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val dnsEntry = context.environment["podcastserver.cluster.dns"]
        val local = context.environment["podcastserver.cluster.local"]

        return !dnsEntry.isNullOrEmpty() && !local.isNullOrEmpty()
    }
}
