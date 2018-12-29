package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.francetv.FranceTvFinder
import com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom.JeuxVideoComFinder
import com.github.davinkevin.podcastserver.manager.worker.sixplay.SixPlayFinder
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.manager.worker.beinsports.BeInSportsFinder
import lan.dk.podcastserver.manager.worker.dailymotion.DailymotionFinder
import lan.dk.podcastserver.manager.worker.gulli.GulliFinder
import lan.dk.podcastserver.manager.worker.itunes.ItunesFinder
import lan.dk.podcastserver.manager.worker.mycanal.MyCanalFinder
import lan.dk.podcastserver.manager.worker.rss.RSSFinder
import lan.dk.podcastserver.manager.worker.tf1replay.TF1ReplayFinder
import lan.dk.podcastserver.manager.worker.youtube.YoutubeFinder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinderSelectorTest {

    @Mock lateinit var beInSportsFinder: BeInSportsFinder
    @Mock lateinit var dailymotionFinder: DailymotionFinder
    @Mock lateinit var franceTvFinder: FranceTvFinder
    @Mock lateinit var gulliFinder: GulliFinder
    @Mock lateinit var itunesFinder: ItunesFinder
    @Mock lateinit var jeuxVideoComFinder: JeuxVideoComFinder
    @Mock lateinit var myCanalFinder: MyCanalFinder
    @Mock lateinit var rssFinder: RSSFinder
    @Mock lateinit var sixPlayFinder: SixPlayFinder
    @Mock lateinit var tf1ReplayFinder: TF1ReplayFinder
    @Mock lateinit var youtubeFinder: YoutubeFinder

    lateinit var finderSelector: FinderSelector

    @BeforeEach
    fun beforeEach() {
        whenever(beInSportsFinder.compatibility(any())).thenCallRealMethod()
        whenever(dailymotionFinder.compatibility(any())).thenCallRealMethod()
        whenever(franceTvFinder.compatibility(any())).thenCallRealMethod()
        whenever(gulliFinder.compatibility(any())).thenCallRealMethod()
        whenever(itunesFinder.compatibility(any())).thenCallRealMethod()
        whenever(jeuxVideoComFinder.compatibility(any())).thenCallRealMethod()
        whenever(myCanalFinder.compatibility(any())).thenCallRealMethod()
        whenever(rssFinder.compatibility(any())).thenCallRealMethod()
        whenever(sixPlayFinder.compatibility(any())).thenCallRealMethod()
        whenever(tf1ReplayFinder.compatibility(any())).thenCallRealMethod()
        whenever(youtubeFinder.compatibility(any())).thenCallRealMethod()

        finderSelector = FinderSelector(setOf(beInSportsFinder, myCanalFinder, dailymotionFinder, franceTvFinder, gulliFinder, itunesFinder, jeuxVideoComFinder, rssFinder, sixPlayFinder, tf1ReplayFinder, youtubeFinder))
    }

    @Test
    fun `should return noop finder if empty url is send to finder`() {
        /* When */
        val finder = finderSelector.of("")
        /* Then */
        assertThat(finder).isEqualTo(FinderSelector.NO_OP_FINDER)
    }

    @Test
    fun `should return noop finder if null url is send to finder`() {
        /* When */
        val finder = finderSelector.of(null)
        /* Then */
        assertThat(finder).isEqualTo(FinderSelector.NO_OP_FINDER)
    }

    @MethodSource("urlToFinderType")
    @DisplayName("should return")
    @ParameterizedTest(name = "{1} finder for {0}")
    fun `should return matching updater`(url: String, type: KClass<*>) {
        /* When */
        val finderClass = finderSelector.of(url)
        /* Then */
        assertThat(finderClass).isInstanceOf(type.java)
    }

    companion object {
        @JvmStatic
        fun urlToFinderType() =
                Stream.of(
                        Arguments.of("http://www.beinsports.com/france/replay/lexpresso", BeInSportsFinder::class),
                        Arguments.of("http://www.dailymotion.com/foo/bar", DailymotionFinder::class),
                        Arguments.of("http://www.france.tv/videos/comment_ca_va_bien.html", FranceTvFinder::class),
                        Arguments.of("http://replay.gulli.fr/videos/foo/bar", GulliFinder::class),
                        Arguments.of("https://itunes.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2", ItunesFinder::class),
                        Arguments.of("http://www.jeuxvideo.com/chroniques-video.htm", JeuxVideoComFinder::class),
                        Arguments.of("http://www.mycanal.fr/c-divertissement/c-le-grand-journal/pid5411-le-grand-journal.html", MyCanalFinder::class),
                        Arguments.of("http://foo.bar.com/to/rss/file.xml", RSSFinder::class),
                        Arguments.of("http://www.6play.fr/videos/foo/bar", SixPlayFinder::class),
                        Arguments.of("http://www.tf1.fr/videos/foo/bar", TF1ReplayFinder::class),
                        Arguments.of("http://www.youtube.com/channel/UC_ioajefokjFAOI", YoutubeFinder::class)
                )
    }

}
