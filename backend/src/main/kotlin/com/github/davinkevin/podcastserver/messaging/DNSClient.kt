package com.github.davinkevin.podcastserver.messaging

import reactor.core.publisher.Flux
import java.net.InetAddress

/**
 * Created by kevin on 02/05/2020
 */
class DNSClient {

    fun allByName(name: String): Flux<InetAddress> {
        return Flux.fromArray(InetAddress.getAllByName(name))
    }

}
