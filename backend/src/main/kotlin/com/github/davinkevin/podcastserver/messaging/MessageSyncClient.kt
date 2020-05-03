package com.github.davinkevin.podcastserver.messaging

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.InetAddress
import javax.annotation.PostConstruct

/**
 * Created by kevin on 02/05/2020
 */
class MessageSyncClient(
        private val message: MessagingTemplate,
        private val dns: DNSClient,
        private val wcb: WebClient.Builder,
        private val cluster: ClusterProperties,
        private val server: ServerProperties
) {

    private val log = LoggerFactory.getLogger(MessageSyncClient::class.java)

    @PostConstruct
    fun postConstruct() {
        log.info("""MessageSyncClient is initialised and will broadcast events to other podcast-server on "${cluster.dns}" dns entry""")
        message
                .messages
                .share()
                .flatMap { m -> dns
                        .allByName(cluster.dns)
                        .filter { it.hostAddress != cluster.local }
                        .map { addr -> m to addr }
                }
                .flatMap { (message, addr) -> send(message, addr) }
                .subscribe()
    }

    private fun <T> send(message: Message<T>, addr: InetAddress): Mono<Void> {
        log.debug("send $message to ${addr.hostAddress}")
        val proto = if(server.ssl != null && server.ssl.isEnabled) "https" else "http"
        return wcb
                .clone()
                .baseUrl("""$proto://${addr.hostAddress}:${server.port}""")
                .build()
                .post()
                .uri("/api/v1/sse/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(convert(message))
                .exchange()
                .then()
    }

}

internal sealed class SyncMessage<T>(val event: String, val body: T)
internal class SyncUpdateMessage(value: Boolean): SyncMessage<Boolean>("updating", value)
internal class SyncWaitingQueueMessage(value: List<DownloadingItemHAL>): SyncMessage<List<DownloadingItemHAL>>("waiting", value)
internal class SyncDownloadingItemMessage(value: DownloadingItemHAL): SyncMessage<DownloadingItemHAL>("downloading", value)

private fun <T> convert(v: Message<T>): SyncMessage<out Any> {
    return when (v) {
        is UpdateMessage -> SyncUpdateMessage(v.value)
        is WaitingQueueMessage -> SyncWaitingQueueMessage(v.value.map(::toDownloadingItemHAL))
        is DownloadingItemMessage -> SyncDownloadingItemMessage(toDownloadingItemHAL(v.value))
    }
}
