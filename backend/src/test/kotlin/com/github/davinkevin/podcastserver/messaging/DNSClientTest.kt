package com.github.davinkevin.podcastserver.messaging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import reactor.test.StepVerifier

/**
 * Created by kevin on 03/05/2020
 */
class DNSClientTest {

    @Nested
    @DisplayName("OnLinux")
    @EnabledOnOs(OS.LINUX)
    inner class OnLinux {

        @Test
        fun `should fetch all by name`() {
            /* Given */
            val dns = DNSClient()
            /* When */
            StepVerifier.create(dns.allByName("localhost"))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.hostAddress).isEqualTo("127.0.0.1")
                    }
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("OnMacOS")
    @EnabledOnOs(OS.MAC)
    inner class OnMacOS {

        @Test
        fun `should fetch all by name`() {
            /* Given */
            val dns = DNSClient()
            /* When */
            StepVerifier.create(dns.allByName("localhost"))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.hostAddress).isEqualTo("127.0.0.1")
                    }
                    .assertNext {
                        assertThat(it.hostAddress).isEqualTo("0:0:0:0:0:0:0:1")
                    }
                    .verifyComplete()
        }

    }

}
