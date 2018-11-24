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
    fun should_get_validator() {
        /* Given */
        /* When */
        val validator = beanConfigScan.validator()
        /* Then */
        assertThat(validator)
                .isNotNull
                .isInstanceOf(LocalValidatorFactoryBean::class.java)
    }

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

    @Test
    fun should_provide_a_converter_from_string_to_vavr_set() {
        /* GIVEN */
        val converter = beanConfigScan.stringToSet()
        val multiValueSeparatedByComma = "foo,bar,another"

        /* WHEN  */
        val strings = converter.convert(multiValueSeparatedByComma)!!.toJavaSet()

        /* THEN  */
        assertThat(strings).contains("foo", "bar", "another")
    }
}
