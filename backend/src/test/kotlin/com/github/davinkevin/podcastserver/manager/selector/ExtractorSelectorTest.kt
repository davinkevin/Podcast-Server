package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.noop.PassThroughExtractor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * Created by kevin on 03/12/2017
 */
@ExtendWith(SpringExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtractorSelectorTest(
    @Autowired val applicationContext: ApplicationContext
) {

    @MockBean private lateinit var passThroughExtractor: PassThroughExtractor

    private lateinit var extractor: ExtractorSelector

    @BeforeEach
    fun beforeEach() {
        val extractors = setOf(passThroughExtractor)

        extractors.forEach { whenever(it.compatibility(any())).thenCallRealMethod() }

        extractor = ExtractorSelector(applicationContext, extractors)
    }

    @Test
    fun `should return no op if empty string`() {
        /* When  */
        val extractorClass = extractor.of(URI("https://foo.bar.com"))
        /* Then  */
        assertThat(extractorClass).isInstanceOf(PassThroughExtractor::class.java)
    }

    @MethodSource("urlToExtractor")
    @DisplayName("should return")
    @ParameterizedTest(name = "{1} finder for {0}")
    fun `should return matching updater`(url: URI, type: KClass<*>) {
        /* When */
        val extractorClass = extractor.of(url)
        /* Then */
        assertThat(extractorClass).isInstanceOf(type.java)
    }

    companion object {
        @JvmStatic
        fun urlToExtractor() =
                Stream.of(
                        Arguments.of(URI("http://www.beinsports.com/france/replay/lexpresso"), PassThroughExtractor::class),
                        Arguments.of(URI("http://www.dailymotion.com/foo/bar"), PassThroughExtractor::class),
                        Arguments.of(URI("http://www.jeuxvideo.com/chroniques-video.htm"), PassThroughExtractor::class),
                        Arguments.of(URI("http://foo.bar.com/to/rss/file.xml"), PassThroughExtractor::class),
                        Arguments.of(URI("http://www.youtube.com/channel/UC_ioajefokjFAOI"), PassThroughExtractor::class)
                )
    }

}
