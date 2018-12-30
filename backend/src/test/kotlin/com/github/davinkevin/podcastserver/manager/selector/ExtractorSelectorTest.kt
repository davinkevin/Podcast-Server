package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.francetv.FranceTvExtractor
import com.github.davinkevin.podcastserver.manager.worker.sixplay.SixPlayExtractor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.manager.worker.gulli.GulliExtractor
import lan.dk.podcastserver.manager.worker.mycanal.MyCanalExtractor
import lan.dk.podcastserver.manager.worker.noop.PassThroughExtractor
import lan.dk.podcastserver.manager.worker.tf1replay.TF1ReplayExtractor
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
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * Created by kevin on 03/12/2017
 */
@ExtendWith(SpringExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtractorSelectorTest {

    @MockBean lateinit var franceTvExtractor: FranceTvExtractor
    @MockBean lateinit var gulliExtractor: GulliExtractor
    @MockBean lateinit var myCanalExtractor: MyCanalExtractor
    @MockBean lateinit var passThroughExtractor: PassThroughExtractor
    @MockBean lateinit var sixPlayExtractor: SixPlayExtractor
    @MockBean lateinit var tf1ReplayExtractor: TF1ReplayExtractor

    @Autowired lateinit var applicationContext: ApplicationContext
    lateinit var extractor: ExtractorSelector

    @BeforeEach
    fun beforeEach() {
        val extractors = setOf(
                franceTvExtractor,
                gulliExtractor,
                myCanalExtractor,
                passThroughExtractor,
                sixPlayExtractor,
                tf1ReplayExtractor)

        extractors.forEach { whenever(it.compatibility(any())).thenCallRealMethod() }

        extractor = ExtractorSelector(applicationContext, extractors
        )
    }

    @Test
    fun `should return no op if empty string`() {
        /* When  */
        val extractorClass = extractor.of("")
        /* Then  */
        assertThat(extractorClass).isEqualTo(ExtractorSelector.NO_OP_EXTRACTOR)
    }

    @MethodSource("urlToExtractor")
    @DisplayName("should return")
    @ParameterizedTest(name = "{1} finder for {0}")
    fun `should return matching updater`(url: String, type: KClass<*>) {
        /* When */
        val finderClass = extractor.of(url)
        /* Then */
        assertThat(finderClass).isInstanceOf(type.java)
    }

    companion object {
        @JvmStatic
        fun urlToExtractor() =
                Stream.of(
                        Arguments.of("http://www.beinsports.com/france/replay/lexpresso", PassThroughExtractor::class),
                        Arguments.of("http://www.dailymotion.com/foo/bar", PassThroughExtractor::class),
                        Arguments.of("http://www.france.tv/videos/comment_ca_va_bien.html", FranceTvExtractor::class),
                        Arguments.of("http://replay.gulli.fr/videos/foo/bar", GulliExtractor::class),
                        Arguments.of("http://www.jeuxvideo.com/chroniques-video.htm", PassThroughExtractor::class),
                        Arguments.of("http://www.mycanal.fr/c-divertissement/c-le-grand-journal/pid5411-le-grand-journal.html", MyCanalExtractor::class),
                        Arguments.of("http://foo.bar.com/to/rss/file.xml", PassThroughExtractor::class),
                        Arguments.of("http://www.6play.fr/videos/foo/bar", SixPlayExtractor::class),
                        Arguments.of("http://www.tf1.fr/videos/foo/bar", TF1ReplayExtractor::class),
                        Arguments.of("http://www.youtube.com/channel/UC_ioajefokjFAOI", PassThroughExtractor::class)
                )
    }

}