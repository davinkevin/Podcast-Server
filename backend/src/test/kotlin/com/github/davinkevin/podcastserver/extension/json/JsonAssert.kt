package com.github.davinkevin.podcastserver.extension.json

import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.JsonAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.RestTestClient

/**
 * Created by kevin on 2019-02-19
 */
fun WebTestClient.BodyContentSpec.assertThatJson(t: JsonAssert.ConfigurableJsonAssert.() -> Unit ): WebTestClient.BodyContentSpec {
    val json = String(returnResult().responseBody!!)
    t(JsonAssertions.assertThatJson(json))
    return this
}
fun RestTestClient.BodyContentSpec.assertThatJson(t: JsonAssert.ConfigurableJsonAssert.() -> Unit ): RestTestClient.BodyContentSpec {
    val json = String(returnResult().responseBody!!)
    t(JsonAssertions.assertThatJson(json))
    return this
}
