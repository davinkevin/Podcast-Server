package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.web.server.Ssl
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.DirectProcessor
import reactor.kotlin.core.publisher.toFlux
import java.net.InetAddress
import java.net.URI
import java.util.*

/**
 * Created by kevin on 03/05/2020
 */
class MessageSyncClientTest {

    val messages = DirectProcessor.create<Message<out Any>>()

    private val local = mock<InetAddress>()
    private val remote = mock<InetAddress>()

    private val message = mock<MessagingTemplate>()
    private val dns = mock<DNSClient>()
    private val cluster = ClusterProperties(local = "10.0.0.1", dns = "ps-headless")
    private val server: ServerProperties = mock<ServerProperties>()
    private val wcb = WebClient
            .builder()
            .clone()
            .apply { remapToMockServer("10.0.0.2:8080").customize(it) }

    @BeforeEach
    fun beforeEach() {
        whenever(local.hostAddress).thenReturn("10.0.0.1")
        whenever(remote.hostAddress).thenReturn("10.0.0.2")

        whenever(message.messages).thenReturn(messages)
        whenever(dns.allByName(cluster.dns)).thenReturn(listOf(local, remote).toFlux())
        whenever(server.port).thenReturn(8080)
    }

    @Nested
    @DisplayName("should forward")
    @ExtendWith(MockServer::class)
    inner class ShouldForward {

        @BeforeEach
        fun beforeEach() {
            whenever(server.ssl).thenReturn(Ssl().apply { isEnabled = false })
        }

        @Test
        fun `update messages`(backend: WireMockServer) {
            /* Given */
            val syncClient = MessageSyncClient(message, dns, wcb, cluster, server)
            backend.stubFor(post("/api/v1/sse/sync")
                    .withRequestBody(equalToJson(""" {"event": "updating", "body":true} """))
                    .willReturn(ok())
            )
            syncClient.postConstruct()

            /* When */
            messages.onNext(UpdateMessage(true))

            /* Then */
            await().atMost(Duration.FIVE_SECONDS).untilAsserted {
                backend.verify(1, postRequestedFor(urlEqualTo("/api/v1/sse/sync")))
            }
        }

        private val item1 = DownloadingItem(
                id = UUID.fromString("33abdbc2-86aa-4a8f-82fe-ff0e3ee735b0"),
                title = "Title 1",
                status = Status.NOT_DOWNLOADED,
                url = URI("https://foo.bar.com/podcast/title-1"),
                progression = 0,
                numberOfFail = 0,
                podcast = DownloadingItem.Podcast(UUID.fromString("bce8c823-f150-4545-81f3-7e892219f230"), "podcast"),
                cover = DownloadingItem.Cover(UUID.fromString("bce8c823-f150-4545-81f3-7e892219f230"), URI("https://foo.bar.com/podcast/title-1.jpg"))
        )
        private val item2 = DownloadingItem(
                id = UUID.fromString("e3e18070-e424-4aa1-abb8-600bb97f76d3"),
                title = "Title 2",
                status = Status.STARTED,
                url = URI("https://foo.bar.com/podcast/title-2"),
                progression = 50,
                numberOfFail = 1,
                podcast = DownloadingItem.Podcast(UUID.fromString("bce8c823-f150-4545-81f3-7e892219f230"), "podcast"),
                cover = DownloadingItem.Cover(UUID.fromString("bf6b1501-c69d-4cb6-a1e0-9b9542c6bbd2"), URI("https://foo.bar.com/podcast/title-2.jpg"))
        )
        private val item3 = DownloadingItem(
                id = UUID.fromString("87dac5eb-214a-4843-ad75-39aa72d292dc"),
                title = "Title 3",
                status = Status.STARTED,
                url = URI("https://foo.bar.com/podcast/title-3"),
                progression = 75,
                numberOfFail = 0,
                podcast = DownloadingItem.Podcast(UUID.fromString("bce8c823-f150-4545-81f3-7e892219f230"), "podcast"),
                cover = DownloadingItem.Cover(UUID.fromString("d7f3f1f7-0b3a-46d5-98db-6e2cd50451f2"), URI("https://foo.bar.com/podcast/title-3.jpg"))
        )
        private val item4 = DownloadingItem(
                id = UUID.fromString("5fbf5846-c1f9-4d2e-a8bf-0d12300c5966"),
                title = "Title 4",
                status = Status.FINISH,
                url = URI("https://foo.bar.com/podcast/title-4"),
                progression = 100,
                numberOfFail = 3,
                podcast = DownloadingItem.Podcast(UUID.fromString("bce8c823-f150-4545-81f3-7e892219f230"), "podcast"),
                cover = DownloadingItem.Cover(UUID.fromString("e2691e66-9ffb-4f7d-811b-79921ae287fe"), URI("https://foo.bar.com/podcast/title-4.jpg"))
        )

        @Test
        fun `downloading messages`(backend: WireMockServer) {
            /* Given */
            val syncClient = MessageSyncClient(message, dns, wcb, cluster, server)
            backend.stubFor(post("/api/v1/sse/sync")
                    .withRequestBody(equalToJson(""" {
                      "event" : "downloading",
                      "body" : {
                        "id" : "33abdbc2-86aa-4a8f-82fe-ff0e3ee735b0",
                        "title" : "Title 1",
                        "status" : "NOT_DOWNLOADED",
                        "url" : "https://foo.bar.com/podcast/title-1",
                        "progression" : 0,
                        "podcast" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "title" : "podcast"
                        },
                        "cover" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "url" : "https://foo.bar.com/podcast/title-1.jpg"
                        },
                        "isDownloaded" : false
                      }
                    }
                    """))
                    .willReturn(ok())
            )
            syncClient.postConstruct()

            /* When */
            messages.onNext(DownloadingItemMessage(item1))

            /* Then */
            await().atMost(Duration.FIVE_SECONDS).untilAsserted {
                backend.verify(1, postRequestedFor(urlEqualTo("/api/v1/sse/sync")))
            }
        }

        @Test
        fun `waiting list messages`(backend: WireMockServer) {
            /* Given */
            val syncClient = MessageSyncClient(message, dns, wcb, cluster, server)
            backend.stubFor(post("/api/v1/sse/sync")
                    .withRequestBody(equalToJson("""{                                                   
                      "event" : "waiting",
                      "body" : [ {
                        "id" : "33abdbc2-86aa-4a8f-82fe-ff0e3ee735b0",
                        "title" : "Title 1",
                        "status" : "NOT_DOWNLOADED",
                        "url" : "https://foo.bar.com/podcast/title-1",
                        "progression" : 0,
                        "podcast" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "title" : "podcast"
                        },
                        "cover" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "url" : "https://foo.bar.com/podcast/title-1.jpg"
                        },
                        "isDownloaded" : false
                      }, {
                        "id" : "e3e18070-e424-4aa1-abb8-600bb97f76d3",
                        "title" : "Title 2",
                        "status" : "STARTED",
                        "url" : "https://foo.bar.com/podcast/title-2",
                        "progression" : 50,
                        "podcast" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "title" : "podcast"
                        },
                        "cover" : {
                          "id" : "bf6b1501-c69d-4cb6-a1e0-9b9542c6bbd2",
                          "url" : "https://foo.bar.com/podcast/title-2.jpg"
                        },
                        "isDownloaded" : false
                      }, {
                        "id" : "87dac5eb-214a-4843-ad75-39aa72d292dc",
                        "title" : "Title 3",
                        "status" : "STARTED",
                        "url" : "https://foo.bar.com/podcast/title-3",
                        "progression" : 75,
                        "podcast" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "title" : "podcast"
                        },
                        "cover" : {
                          "id" : "d7f3f1f7-0b3a-46d5-98db-6e2cd50451f2",
                          "url" : "https://foo.bar.com/podcast/title-3.jpg"
                        },
                        "isDownloaded" : false
                      }, {
                        "id" : "5fbf5846-c1f9-4d2e-a8bf-0d12300c5966",
                        "title" : "Title 4",
                        "status" : "FINISH",
                        "url" : "https://foo.bar.com/podcast/title-4",
                        "progression" : 100,
                        "podcast" : {
                          "id" : "bce8c823-f150-4545-81f3-7e892219f230",
                          "title" : "podcast"
                        },
                        "cover" : {
                          "id" : "e2691e66-9ffb-4f7d-811b-79921ae287fe",
                          "url" : "https://foo.bar.com/podcast/title-4.jpg"
                        },
                        "isDownloaded" : true
                      } ]
                    }"""))
                    .willReturn(ok())
            )
            syncClient.postConstruct()

            /* When */
            messages.onNext(WaitingQueueMessage(listOf(item1, item2, item3, item4)))

            /* Then */
            await().atMost(Duration.FIVE_SECONDS).untilAsserted {
                backend.verify(1, postRequestedFor(urlEqualTo("/api/v1/sse/sync")))
            }
        }

    }

    @Nested
    @DisplayName("should forward to")
    @ExtendWith(MockServer::class)
    inner class ShouldForwardTo {

        @Test
        fun `http backend due to ssl properties null`(backend: WireMockServer) {
            /* Given */
            whenever(server.ssl).thenReturn(null)
            val syncClient = MessageSyncClient(message, dns, wcb, cluster, server)
            backend.stubFor(post("/api/v1/sse/sync")
                    .withRequestBody(equalToJson(""" {"event": "updating", "body":true} """))
                    .willReturn(ok())
            )
            syncClient.postConstruct()

            /* When */
            messages.onNext(UpdateMessage(true))

            /* Then */
            await().atMost(Duration.FIVE_SECONDS).untilAsserted {
                backend.verify(1, postRequestedFor(urlEqualTo("/api/v1/sse/sync")))
            }
        }


        @Test
        fun `https backend`(backend: WireMockServer) {
            /* Given */
            whenever(server.ssl).thenReturn(Ssl().apply { isEnabled = true })
            val syncClient = MessageSyncClient(message, dns, wcb, cluster, server)
            backend.stubFor(post("/api/v1/sse/sync")
                    .withRequestBody(equalToJson(""" {"event": "updating", "body":true} """))
                    .willReturn(ok())
            )
            syncClient.postConstruct()

            /* When */
            messages.onNext(UpdateMessage(true))

            /* Then */
            await().atMost(Duration.FIVE_SECONDS).untilAsserted {
                backend.verify(1, postRequestedFor(urlEqualTo("/api/v1/sse/sync")))
            }
        }
    }

}
