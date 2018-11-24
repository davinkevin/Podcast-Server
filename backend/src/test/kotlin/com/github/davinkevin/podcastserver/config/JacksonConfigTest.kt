package com.github.davinkevin.podcastserver.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@ExtendWith(SpringExtension::class)
@Import(JacksonConfig::class)
class JacksonConfigTest {

    @Autowired private lateinit var mapper: ObjectMapper

    @Test
    fun `should serialize date as iso 8601`() {
        /* Given */
        val date = ZonedDateTime.of(2016, 6, 15, 2, 43, 15, 826, ZoneId.of("Europe/Paris"))
        /* When */
        val jsonDate = mapper.writeValueAsString(date)
        /* Then */
        assertThat(jsonDate).isEqualTo(""""2016-06-15T02:43:15.000000826+02:00"""")
    }
}
