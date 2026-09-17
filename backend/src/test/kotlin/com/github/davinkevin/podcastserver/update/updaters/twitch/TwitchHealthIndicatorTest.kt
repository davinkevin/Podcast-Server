package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.MockServer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.health.contributor.Status
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Created by kevin on 17/09/2026
 */
@ExtendWith(MockitoExtension::class)
class TwitchHealthIndicatorTest {

    private val token = mock<TwitchTokenProvider>()
    private val indicator = TwitchHealthIndicator(token)

    private val obtainedAt = Instant.parse("2026-09-17T10:00:00Z")
    private val expiresAt = Instant.parse("2026-11-14T10:00:00Z")

    @Test
    fun `should be up with a token held`() {
        /* Given */
        whenever(token.lastOutcome())
            .thenReturn(TwitchTokenProvider.Outcome.Success(obtainedAt = obtainedAt, expiresAt = expiresAt))

        /* When */
        val health = indicator.health()

        /* Then */
        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "token" to "present",
                "obtainedAt" to obtainedAt.toString(),
                "expiresAt" to expiresAt.toString(),
            )
        )
    }

    @Test
    fun `should be up when no negotiation has been attempted yet`() {
        /* Given */
        whenever(token.lastOutcome()).thenReturn(TwitchTokenProvider.Outcome.NotAttempted)

        /* When */
        val health = indicator.health()

        /* Then */
        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "token" to "absent",
                "reason" to "no token negotiated yet",
            )
        )
    }

    @Test
    fun `should be down when the last negotiation failed`() {
        /* Given */
        whenever(token.lastOutcome())
            .thenReturn(TwitchTokenProvider.Outcome.Failure(lastAttemptAt = obtainedAt, reason = "401 Unauthorized"))

        /* When */
        val health = indicator.health()

        /* Then */
        assertThat(health.status).isEqualTo(Status.DOWN)
        assertThat(health.details).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "token" to "absent",
                "lastAttemptAt" to obtainedAt.toString(),
                "reason" to "401 Unauthorized",
            )
        )
    }

    @Nested
    @ExtendWith(MockServer::class)
    inner class WithoutAnyIo {

        @Test
        fun `should not call twitch at all`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(
                post("/oauth2/token")
                    .willReturn(okJson("""{"access_token":"a-token","expires_in":5000000,"token_type":"bearer"}"""))
            )
            val provider = TwitchTokenProvider(
                rc = RestClient.builder().baseUrl("http://localhost:5555").build(),
                clientId = "client-id",
                clientSecret = "client-secret",
                clock = Clock.fixed(obtainedAt, ZoneId.of("UTC")),
            )
            provider.token()
            backend.resetRequests()

            /* When */
            val health = TwitchHealthIndicator(provider).health()

            /* Then */
            assertThat(health.status).isEqualTo(Status.UP)
            backend.verify(0, postRequestedFor(urlEqualTo("/oauth2/token")))
            assertThat(backend.allServeEvents).isEmpty()
        }
    }
}
