package com.github.davinkevin.podcastserver.extension.assertthat

import org.assertj.core.api.SoftAssertions

fun assertAll(block: SoftAssertions.() -> Unit) =
    SoftAssertions()
    .apply(block)
    .assertAll()
