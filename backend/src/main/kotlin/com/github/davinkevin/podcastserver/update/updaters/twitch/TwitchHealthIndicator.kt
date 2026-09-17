package com.github.davinkevin.podcastserver.update.updaters.twitch

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator

/**
 * Reports whether the server is able to talk to twitch.
 *
 * Token negotiation and renewal are invisible from the outside, so they are exposed
 * here. This indicator performs no I/O at all: it only reads what
 * [TwitchTokenProvider] recorded, which keeps the probes free.
 *
 * It is deliberately absent from the `liveness` and `readiness` groups — a twitch
 * outage or a revoked secret turns the aggregate health red without taking the pod
 * down.
 */
class TwitchHealthIndicator(
    private val token: TwitchTokenProvider,
) : HealthIndicator {

    override fun health(): Health = when (val outcome = token.lastOutcome()) {
        is TwitchTokenProvider.Outcome.NotAttempted -> Health.up()
            .withDetail("token", "absent")
            .withDetail("reason", "no token negotiated yet")
            .build()

        is TwitchTokenProvider.Outcome.Success -> Health.up()
            .withDetail("token", "present")
            .withDetail("obtainedAt", outcome.obtainedAt.toString())
            .withDetail("expiresAt", outcome.expiresAt.toString())
            .build()

        is TwitchTokenProvider.Outcome.Failure -> Health.down()
            .withDetail("token", "absent")
            .withDetail("lastAttemptAt", outcome.lastAttemptAt.toString())
            .withDetail("reason", outcome.reason)
            .build()
    }
}
