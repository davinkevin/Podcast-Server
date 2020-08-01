package com.github.davinkevin.podcastserver

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.TestPropertySources

/**
 * Created by kevin on 13/03/2020
 */
@SpringBootTest
@EnabledOnOs(OS.LINUX)
@TestPropertySource(properties = [
    "podcastserver.externaltools.ffmpeg=/bin/echo",
    "podcastserver.externaltools.ffprobe=/bin/echo"
])
class PodcastServerApplicationTests {
    @Test fun contextLoads() {}
}
