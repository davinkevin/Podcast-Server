package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.MockServer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Created by kevin on 17/09/2026
 */
@ExtendWith(MockServer::class)
class TwitchTokenProviderTest {

    private val now = Instant.parse("2026-09-17T10:00:00Z")

    private fun providerWith(clock: Clock = Clock.fixed(now, ZoneId.of("UTC"))) = TwitchTokenProvider(
        rc = RestClient.builder().baseUrl("http://localhost:5555").build(),
        clientId = "client-id",
        clientSecret = "client-secret",
        clock = clock,
    )

    private fun tokenResponse(accessToken: String, expiresIn: Long = 5_000_000) = okJson(
        """{"access_token":"$accessToken","expires_in":$expiresIn,"token_type":"bearer"}"""
    )

    @Nested
    @DisplayName("should provide a token")
    inner class ShouldProvideAToken {

        @Test
        fun `negotiated against id twitch tv`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(post("/oauth2/token").willReturn(tokenResponse("a-token")))
            val provider = providerWith()

            /* When */
            val token = provider.token()

            /* Then */
            assertThat(token).isEqualTo("a-token")
            backend.verify(
                1, postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("client_id=client-id"))
                    .withRequestBody(containing("client_secret=client-secret"))
                    .withRequestBody(containing("grant_type=client_credentials"))
            )
        }

        @Test
        fun `reused across calls instead of being negotiated every time`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(post("/oauth2/token").willReturn(tokenResponse("a-token")))
            val provider = providerWith()

            /* When */
            val tokens = (1..5).map { provider.token() }

            /* Then */
            assertThat(tokens).containsOnly("a-token")
            backend.verify(1, postRequestedFor(urlEqualTo("/oauth2/token")))
        }

        @Test
        fun `negotiated again once expired`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(
                post("/oauth2/token")
                    .inScenario("token")
                    .whenScenarioStateIs("Started")
                    .willReturn(tokenResponse("first-token", expiresIn = 3_600))
                    .willSetStateTo("second")
            )
            backend.stubFor(
                post("/oauth2/token")
                    .inScenario("token")
                    .whenScenarioStateIs("second")
                    .willReturn(tokenResponse("second-token"))
            )
            val clock = MutableClock(now)
            val provider = providerWith(clock)

            /* When */
            val first = provider.token()
            clock.advance(Duration.ofHours(2))
            val second = provider.token()

            /* Then */
            assertThat(first).isEqualTo("first-token")
            assertThat(second).isEqualTo("second-token")
            backend.verify(2, postRequestedFor(urlEqualTo("/oauth2/token")))
        }
    }

    @Nested
    @DisplayName("should retry on 401")
    inner class ShouldRetryOn401 {

        private fun helixCall(token: String): String = RestClient.builder()
            .baseUrl("http://localhost:5555")
            .build()
            .get()
            .uri("/helix/users")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .body<String>()!!

        @Test
        fun `by negotiating a new token exactly once and replaying the call`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(post("/oauth2/token").willReturn(tokenResponse("a-token")))
            backend.stubFor(
                get("/helix/users")
                    .inScenario("401")
                    .whenScenarioStateIs("Started")
                    .willReturn(unauthorized())
                    .willSetStateTo("authorized")
            )
            backend.stubFor(
                get("/helix/users")
                    .inScenario("401")
                    .whenScenarioStateIs("authorized")
                    .willReturn(okJson("""{"data":[]}"""))
            )
            val provider = providerWith()

            /* When */
            val response = provider.withRetryOn401(::helixCall)

            /* Then */
            assertThat(response).isEqualTo("""{"data":[]}""")
            backend.verify(2, postRequestedFor(urlEqualTo("/oauth2/token")))
            backend.verify(2, getRequestedFor(urlEqualTo("/helix/users")))
        }

        @Test
        fun `only once, a second 401 is a hard failure`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(post("/oauth2/token").willReturn(tokenResponse("a-token")))
            backend.stubFor(get("/helix/users").willReturn(unauthorized()))
            val provider = providerWith()

            /* When */
            assertThatThrownBy { provider.withRetryOn401(::helixCall) }
                /* Then */
                .isInstanceOf(HttpClientErrorException.Unauthorized::class.java)

            backend.verify(2, getRequestedFor(urlEqualTo("/helix/users")))
        }
    }

    @Nested
    @DisplayName("should record its last outcome")
    inner class ShouldRecordItsLastOutcome {

        @Test
        fun `as not attempted before any negotiation`(backend: WireMockServer) {
            /* Given */
            val provider = providerWith()

            /* When */
            val outcome = provider.lastOutcome()

            /* Then */
            assertThat(outcome).isEqualTo(TwitchTokenProvider.Outcome.NotAttempted)
            backend.verify(0, postRequestedFor(urlEqualTo("/oauth2/token")))
        }

        @Test
        fun `as a success, with the obtention and expiry dates`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(post("/oauth2/token").willReturn(tokenResponse("a-token", expiresIn = 3_600)))
            val provider = providerWith()

            /* When */
            provider.token()

            /* Then */
            assertThat(provider.lastOutcome()).isEqualTo(
                TwitchTokenProvider.Outcome.Success(
                    obtainedAt = now,
                    expiresAt = now.plusSeconds(3_600 - 60),
                )
            )
        }

        @Test
        fun `as a failure, with a readable reason`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(post("/oauth2/token").willReturn(unauthorized()))
            val provider = providerWith()

            /* When */
            assertThatThrownBy { provider.token() }
                /* Then */
                .isInstanceOf(HttpClientErrorException.Unauthorized::class.java)

            val outcome = provider.lastOutcome()
            assertThat(outcome).isInstanceOf(TwitchTokenProvider.Outcome.Failure::class.java)
            outcome as TwitchTokenProvider.Outcome.Failure
            assertThat(outcome.lastAttemptAt).isEqualTo(now)
            assertThat(outcome.reason).contains("401")
        }
    }
}

private class MutableClock(private var current: Instant) : Clock() {
    fun advance(duration: Duration) {
        current = current.plus(duration)
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = current
}
