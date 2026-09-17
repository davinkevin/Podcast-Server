package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant

/**
 * Created by kevin on 17/09/2026
 *
 * A twitch outage must be visible without taking the pod down, so the indicator stays
 * out of the probes: `liveness` and `readiness` are declared explicitly in
 * `application.yml`, and this test is what keeps someone from "fixing" that by adding
 * `twitch` to either list.
 */
@SpringBootTest(
    properties = [
        "podcastserver.storage.url=http://localhost:9000/",
        "podcastserver.storage.bucket=bucket",
        "podcastserver.api.twitch.client-id=client-id",
        "podcastserver.api.twitch.client-secret=client-secret",
    ]
)
@ExtendWith(MockS3Server::class)
@EnabledOnOs(OS.LINUX, OS.MAC)
class TwitchHealthGroupsTest(
    @Autowired private val health: HealthEndpoint,
) {

    /**
     * The token provider is mocked rather than the indicator itself: overriding a
     * health indicator bean keeps its name but drops it from the health registry, so
     * the groups would no longer be exercised.
     */
    @MockitoBean lateinit var token: TwitchTokenProvider

    @Test
    fun `should keep the probes up while twitch is down`() {
        /* Given */
        whenever(token.lastOutcome()).thenReturn(
            TwitchTokenProvider.Outcome.Failure(
                lastAttemptAt = Instant.parse("2026-09-17T10:00:00Z"),
                reason = "401 Unauthorized",
            )
        )

        /* When */
        val aggregate = health.health()
        val twitchOnly = health.healthForPath("twitch")!!
        val liveness = health.healthForPath("liveness")!!
        val readiness = health.healthForPath("readiness")!!

        /* Then */
        assertAll {
            assertThat(twitchOnly.status).isEqualTo(Status.DOWN)
            assertThat(aggregate.status).isEqualTo(Status.DOWN)

            assertThat(liveness.status).isEqualTo(Status.UP)
            assertThat(readiness.status).isEqualTo(Status.UP)
        }
    }
}

private class MockS3Server : BeforeAllCallback, AfterAllCallback {

    private lateinit var server: WireMockServer

    override fun beforeAll(p0: ExtensionContext) {
        server = WireMockServer(9000).apply {
            start()
            stubFor(head(urlEqualTo("/bucket")).willReturn(ok()))
        }
    }

    override fun afterAll(context: ExtensionContext) {
        server.stop()
    }
}
