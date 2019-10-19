package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.service.TikaProbeContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.nio.file.Paths

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
class BeanConfigScanTest {

    private val beanConfigScan: BeanConfigScan = BeanConfigScan()

    @Test
    @Throws(NoSuchMethodException::class)
    fun should_have_tika_probecontentType() {
        /* Given */
        /* When */
        val tikaProbeContentType = beanConfigScan.tikaProbeContentType()

        /* Then */
        assertThat(tikaProbeContentType)
                .isNotNull
                .isInstanceOf(TikaProbeContentType::class.java)
    }

    @Test
    fun should_provide_a_converter_from_string_to_path() {
        /* Given */
        val c = beanConfigScan.pathConverter()
        val path = "/tmp"

        /* When */
        val convertedPath = c.convert(path)

        /* Then */
        assertThat(convertedPath)
                .isEqualTo(Paths.get(path))
    }
}
