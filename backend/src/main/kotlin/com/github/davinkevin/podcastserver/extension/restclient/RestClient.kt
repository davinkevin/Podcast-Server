package com.github.davinkevin.podcastserver.extension.restclient

import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestClient
import java.nio.charset.Charset

fun RestClient.Builder.withStringUTF8MessageConverter(): RestClient.Builder =
    this.messageConverters { it.addFirst(StringHttpMessageConverter(Charset.forName("UTF-8"))) }