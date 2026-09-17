package com.github.davinkevin.podcastserver.update.updaters.twitch

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * No credentials ⇒ no twitch bean at all.
 *
 * `@ConditionalOnProperty` is not enough here: an explicitly empty value counts as
 * present, and blank placeholders do reach the application through the deployment
 * overlays.
 */
class TwitchCredentialsCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val environment = context.environment

        val clientId = environment.getProperty("podcastserver.api.twitch.client-id", "")
        val clientSecret = environment.getProperty("podcastserver.api.twitch.client-secret", "")

        return clientId.isNotBlank() && clientSecret.isNotBlank()
    }
}
