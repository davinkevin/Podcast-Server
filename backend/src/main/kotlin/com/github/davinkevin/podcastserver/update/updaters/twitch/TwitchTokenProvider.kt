package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Clock
import java.time.Instant

/**
 * Hands out a valid twitch app access token, and owns the caching and the renewal
 * behind that.
 *
 * The token is valid for weeks, so it is negotiated once and reused across updates
 * instead of being asked for on every helix call. It also records the outcome of its
 * last negotiation, so [TwitchHealthIndicator] can report it without doing any I/O.
 */
class TwitchTokenProvider(
    private val rc: RestClient,
    private val clientId: String,
    private val clientSecret: String,
    private val clock: Clock,
) {

    private val lock = Any()
    private var cached: CachedToken? = null
    private var outcome: Outcome = Outcome.NotAttempted

    fun token(): String = synchronized(lock) {
        val current = cached

        if (current != null && clock.instant().isBefore(current.expiresAt)) {
            return current.value
        }

        negotiate().value
    }

    fun invalidate(): Unit = synchronized(lock) { cached = null }

    fun lastOutcome(): Outcome = synchronized(lock) { outcome }

    /**
     * Runs [call] with a valid token. On a `401` the cached token is dropped, a new one
     * is negotiated and the call is replayed exactly once — a second `401` is a hard
     * failure and propagates.
     */
    fun <T> withRetryOn401(call: (String) -> T): T = try {
        call(token())
    } catch (_: HttpClientErrorException.Unauthorized) {
        invalidate()
        call(token())
    }

    private fun negotiate(): CachedToken {
        val attemptedAt = clock.instant()

        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("grant_type", "client_credentials")
        }

        val response = try {
            rc.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body<TwitchTokenResponse>()
                ?: error("no body returned by id.twitch.tv")
        } catch (e: Exception) {
            cached = null
            outcome = Outcome.Failure(attemptedAt, e.message ?: e.javaClass.simpleName)
            throw e
        }

        val token = CachedToken(
            value = response.accessToken,
            expiresAt = attemptedAt.plusSeconds(response.expiresIn).minusSeconds(EXPIRY_MARGIN_IN_SECONDS),
        )

        cached = token
        outcome = Outcome.Success(obtainedAt = attemptedAt, expiresAt = token.expiresAt)

        return token
    }

    private class CachedToken(val value: String, val expiresAt: Instant)

    sealed interface Outcome {
        data object NotAttempted : Outcome
        data class Success(val obtainedAt: Instant, val expiresAt: Instant) : Outcome
        data class Failure(val lastAttemptAt: Instant, val reason: String) : Outcome
    }

    companion object {
        /** renew slightly before the real expiry, so an in-flight call can't race it */
        private const val EXPIRY_MARGIN_IN_SECONDS = 60L
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TwitchTokenResponse(
    @param:JsonProperty("access_token") val accessToken: String,
    @param:JsonProperty("expires_in") val expiresIn: Long,
    @param:JsonProperty("token_type") val tokenType: String = "bearer",
)
