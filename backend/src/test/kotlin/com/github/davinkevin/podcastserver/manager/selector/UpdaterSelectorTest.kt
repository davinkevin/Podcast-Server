package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.dailymotion.DailymotionUpdater
import com.github.davinkevin.podcastserver.manager.worker.gulli.GulliUpdater
import com.github.davinkevin.podcastserver.manager.worker.mycanal.MyCanalUpdater
import com.github.davinkevin.podcastserver.update.updaters.rss.RSSUpdater
import com.github.davinkevin.podcastserver.manager.worker.sixplay.SixPlayUpdater
import com.github.davinkevin.podcastserver.manager.worker.tf1replay.TF1ReplayUpdater
import com.github.davinkevin.podcastserver.manager.worker.upload.UploadUpdater
import com.github.davinkevin.podcastserver.update.updaters.francetv.FranceTvUpdater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByApiUpdater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByXmlUpdater
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
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.net.URI
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdaterSelectorTest {

    private lateinit var  updaterSelector: UpdaterSelector

    @Mock lateinit var dailymotionUpdater: DailymotionUpdater
    @Mock lateinit var franceTvUpdater: FranceTvUpdater
    @Mock lateinit var gulliUpdater: GulliUpdater
    @Mock lateinit var myCanalUpdater: MyCanalUpdater
    @Mock lateinit var rssUpdater: RSSUpdater
    @Mock lateinit var sixPlayUpdater: SixPlayUpdater
    @Mock lateinit var tf1ReplayUpdater: TF1ReplayUpdater
    @Mock lateinit var uploadUpdater: UploadUpdater
    @Mock lateinit var youtubeByXmlUpdater: YoutubeByXmlUpdater
    @Mock lateinit var youtubeByApiUpdater: YoutubeByApiUpdater

    @BeforeEach
    fun beforeEach() {
        /* Given */
        val updaters = setOf(
                dailymotionUpdater,
                franceTvUpdater, 
                gulliUpdater,
                myCanalUpdater, 
                rssUpdater, 
                sixPlayUpdater,
                tf1ReplayUpdater, 
                uploadUpdater, 
                youtubeByXmlUpdater,
                youtubeByApiUpdater
        )

        updaters.forEach {
            whenever(it.compatibility(any())).thenCallRealMethod()
            whenever(it.type()).thenCallRealMethod()
        }

        updaterSelector = UpdaterSelector(updaters)
    }

    @MethodSource("urlToUpdater")
    @DisplayName("should return")
    @ParameterizedTest(name = "{1} updater for {0}")
    fun `should return matching updater`(url: String, type: String) {
        /* When */
        val updaterClass = updaterSelector.of(URI(url))
        /* Then */
        assertThat(updaterClass.type().key).isEqualTo(type)
    }

    @Test
    fun `should serve types`() {
        /* Given */
        val uTypes = setOf(
                dailymotionUpdater,
                franceTvUpdater,
                gulliUpdater,
                myCanalUpdater,
                rssUpdater,
                sixPlayUpdater,
                tf1ReplayUpdater,
                youtubeByXmlUpdater,
                youtubeByApiUpdater
        )
                .map { it.type() }


        /* When */
        val types = updaterSelector.types()

        /* Then */
        assertThat(types)
                .isNotEmpty
                .hasSize(9)
                .containsAll(uTypes)
    }

    companion object {
        @JvmStatic
        fun urlToUpdater() =
                Stream.of(
                        Arguments.of("http://www.dailymotion.com/showname", "Dailymotion"),
                        Arguments.of("http://www.france.tv/show/for/dummies", "FranceTv"),
                        Arguments.of("http://replay.gulli.fr/showname", "Gulli"),
                        Arguments.of("http://www.mycanal.fr/show/for/dummies", "MyCanal"),
                        Arguments.of("http://www.link.to.rss/feeds", "RSS"),
                        Arguments.of("http://www.6play.fr/turbo_test", "SixPlay"),
                        Arguments.of("http://www.tf1.fr/title", "TF1Replay"),
                        Arguments.of("http://www.youtube.com/user/fakeUser", "Youtube")
                )
    }
}
