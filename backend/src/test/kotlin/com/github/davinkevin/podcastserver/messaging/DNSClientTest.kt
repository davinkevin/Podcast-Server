package com.github.davinkevin.podcastserver.messaging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

/**
 * Created by kevin on 03/05/2020
 */
class DNSClientTest {

    @Test
    fun `should fetch all by name`() {
        /* Given */
        val dns = DNSClient()
        /* When */
        StepVerifier.create(dns
                .allByName("localhost")
                .map { it.hostAddress }
                .collectList()
        )
                /* Then */
                .expectSubscription()
                .assertNext {
                    assertThat(it).contains("127.0.0.1")
                }
                .verifyComplete()
    }
}
