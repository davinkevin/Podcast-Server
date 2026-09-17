package com.github.davinkevin.podcastserver.find.finders.twitch

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.time.Clock

/**
 * Created by kevin on 17/09/2026
 */
@NestedSpringTest
@TestPropertySource(
    properties = [
        "podcastserver.api.twitch.client-id=client-id",
        "podcastserver.api.twitch.client-secret=client-secret",
    ]
)
class TwitchFinderTest(
    @Autowired val finder: TwitchFinder,
) {

    @MockitoBean lateinit var image: ImageService

    private val profileImage =
        URI("https://static-cdn.jtvnw.net/jtv_user_pictures/8a6381c7-d0c0-4576-b179-38bd5ce1d6af-profile_image-300x300.png")
    private val offlineImage =
        URI("https://static-cdn.jtvnw.net/jtv_user_pictures/3f13ab61-ec78-4fe6-8481-8682cb3b0ac2-channel_offline_image-1920x1080.png")

    private fun WireMockServer.stubToken() = stubFor(
        post("/oauth2/token").willReturn(okJson(fileAsString("/remote/podcast/twitch/token.json")))
    )

    private fun WireMockServer.stubUser(fixture: String) = stubFor(
        get(urlEqualTo("/helix/users?login=twitchdev"))
            .withHeader("Client-Id", equalTo("client-id"))
            .withHeader("Authorization", equalTo("Bearer an-app-access-token"))
            .willReturn(okJson(fileAsString("/remote/podcast/twitch/$fixture")))
    )

    @Nested
    @DisplayName("should find")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        @Test
        fun `information about a twitch channel from its url`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser("user.json")
            whenever(image.fetchCoverInformation(profileImage))
                .thenReturn(CoverInformation(300, 300, profileImage))

            /* When */
            val podcast = finder.findPodcastInformation("https://www.twitch.tv/twitchdev")!!

            /* Then */
            assertAll {
                assertThat(podcast.title).isEqualTo("TwitchDev")
                assertThat(podcast.description)
                    .isEqualTo("Supporting third-party developers building Twitch integrations from chatbots to game integrations.")
                assertThat(podcast.type).isEqualTo("Twitch")
                assertThat(podcast.url).isEqualTo(URI("https://www.twitch.tv/twitchdev"))
                assertThat(podcast.cover!!.url).isEqualTo(profileImage)
            }
        }

        @Test
        fun `falling back on the offline image when the profile one is missing`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser("user.without-profile-image.json")
            whenever(image.fetchCoverInformation(offlineImage))
                .thenReturn(CoverInformation(1920, 1080, offlineImage))

            /* When */
            val podcast = finder.findPodcastInformation("https://www.twitch.tv/twitchdev")!!

            /* Then */
            assertThat(podcast.cover!!.url).isEqualTo(offlineImage)
        }

        @Test
        fun `without any cover when the channel has no image at all`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser("user.without-image.json")

            /* When */
            val podcast = finder.findPodcastInformation("https://www.twitch.tv/twitchdev")!!

            /* Then */
            assertAll {
                assertThat(podcast.cover).isNull()
                assertThat(podcast.description).isEmpty()
            }
        }

        @Test
        fun `nothing for an unknown login`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser("user.unknown.json")

            /* When */
            val podcast = finder.findPodcastInformation("https://www.twitch.tv/twitchdev")

            /* Then */
            assertThat(podcast).isNull()
        }

        @Test
        fun `nothing for an url which is not a channel`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()

            /* When */
            val podcast = finder.findPodcastInformation("https://www.twitch.tv/videos/123456")

            /* Then */
            assertAll {
                assertThat(podcast).isNull()
                backend.verify(0, postRequestedFor(urlEqualTo("/oauth2/token")))
            }
        }
    }

    @DisplayName("should be compatible with")
    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "https://www.twitch.tv/twitchdev",
            "https://www.twitch.tv/twitchdev/videos",
            "https://www.twitch.tv/twitchdev/videos?filter=archives&sort=time",
            "https://m.twitch.tv/twitchdev",
            "https://twitch.tv/twitchdev",
            "http://www.twitch.tv/twitchdev/",
        ]
    )
    fun `should be compatible with`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @DisplayName("should not be compatible with")
    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "https://www.twitch.tv/videos/1234567890",
            "https://www.twitch.tv/directory/game/Minecraft",
            "https://www.twitch.tv/settings/profile",
            "https://www.twitch.tv/subscriptions",
            "https://www.twitch.tv/p/about",
            "https://www.twitch.tv/team/twitchdev",
            "https://www.twitch.tv/collections/abcdef",
            "https://www.twitch.tv/twitchdev/clip/AmazonianKnottyLapwing",
            "https://www.twitch.tv/",
            "http://foo.bar.com/",
        ]
    )
    fun `should not be compatible with`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    @TestConfiguration
    @Import(
        TwitchFinderConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration {
        @Bean fun clock(): Clock = Clock.systemUTC()
        @Bean fun remapHelixToMockServer() = remapRestClientToMockServer("api.twitch.tv")
        @Bean fun remapIdToMockServer() = remapRestClientToMockServer("id.twitch.tv")
    }
}
