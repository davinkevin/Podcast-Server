package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.worker.sixplay.SixPlayUpdater
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.manager.worker.beinsports.BeInSportsUpdater
import lan.dk.podcastserver.manager.worker.dailymotion.DailymotionUpdater
import com.github.davinkevin.podcastserver.manager.worker.francetv.FranceTvUpdater
import lan.dk.podcastserver.manager.worker.gulli.GulliUpdater
import com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom.JeuxVideoComUpdater
import lan.dk.podcastserver.manager.worker.mycanal.MyCanalUpdater
import lan.dk.podcastserver.manager.worker.rss.RSSUpdater
import lan.dk.podcastserver.manager.worker.tf1replay.TF1ReplayUpdater
import lan.dk.podcastserver.manager.worker.upload.UploadUpdater
import lan.dk.podcastserver.manager.worker.youtube.YoutubeUpdater
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdaterSelectorTest {

    private lateinit var  updaterSelector: UpdaterSelector

    @Mock lateinit var beInSportsUpdater: BeInSportsUpdater
    @Mock lateinit var dailymotionUpdater: DailymotionUpdater
    @Mock lateinit var franceTvUpdater: FranceTvUpdater
    @Mock lateinit var gulliUpdater: GulliUpdater
    @Mock lateinit var jeuxVideoComUpdater: JeuxVideoComUpdater
    @Mock lateinit var myCanalUpdater: MyCanalUpdater
    @Mock lateinit var rssUpdater: RSSUpdater
    @Mock lateinit var sixPlayUpdater: SixPlayUpdater
    @Mock lateinit var tf1ReplayUpdater: TF1ReplayUpdater
    @Mock lateinit var uploadUpdater: UploadUpdater
    @Mock lateinit var youtubeUpdater: YoutubeUpdater

    @BeforeEach
    fun beforeEach() {
        /* Given */
        val updaters = setOf(
                beInSportsUpdater, 
                dailymotionUpdater, 
                franceTvUpdater, 
                gulliUpdater,
                jeuxVideoComUpdater, 
                myCanalUpdater, 
                rssUpdater, 
                sixPlayUpdater,
                tf1ReplayUpdater, 
                uploadUpdater, 
                youtubeUpdater
        )

        updaters.forEach {
            whenever(it.compatibility(ArgumentMatchers.anyString())).thenCallRealMethod()
            whenever(it.type()).thenCallRealMethod()
        }

        updaterSelector = UpdaterSelector(updaters)
    }

    @MethodSource("urlToUpdater")
    @DisplayName("should return")
    @ParameterizedTest(name = "{1} updater for {0}")
    fun `should return matching updater`(url: String, type: String) {
        /* When */
        val updaterClass = updaterSelector.of(url)
        /* Then */
        assertThat(updaterClass.type().key()).isEqualTo(type)
    }

    @Test
    fun `should reject empty or null url`() {
        /* When */
        assertThat(updaterSelector.of(null)).isEqualTo(UpdaterSelector.NO_OP_UPDATER)
        assertThat(updaterSelector.of("")).isEqualTo(UpdaterSelector.NO_OP_UPDATER)
    }

    @Test
    fun `should serve types`() {
        /* Given */
        val uTypes = setOf(
                beInSportsUpdater,
                dailymotionUpdater,
                franceTvUpdater,
                gulliUpdater,
                jeuxVideoComUpdater,
                myCanalUpdater,
                rssUpdater,
                sixPlayUpdater,
                tf1ReplayUpdater,
                youtubeUpdater
        )
                .map { it.type() }


        /* When */
        val types = updaterSelector.types()

        /* Then */
        assertThat(types)
                .isNotEmpty
                .hasSize(11)
                .containsAll(uTypes)
    }

    companion object {
        @JvmStatic
        fun urlToUpdater() =
                Stream.of(
                        Arguments.of("http://www.beinsports.com/replay/category/3361/name/lexpresso", "BeInSports"),
                        Arguments.of("http://www.dailymotion.com/showname", "Dailymotion"),
                        Arguments.of("http://www.france.tv/show/for/dummies", "FranceTv"),
                        Arguments.of("http://replay.gulli.fr/showname", "Gulli"),
                        Arguments.of("http://www.jeuxvideo.com/show/for/dummies", "JeuxVideoCom"),
                        Arguments.of("http://www.mycanal.fr/show/for/dummies", "MyCanal"),
                        Arguments.of("http://www.link.to.rss/feeds", "RSS"),
                        Arguments.of("http://www.6play.fr/turbo_test", "SixPlay"),
                        Arguments.of("http://www.tf1.fr/title", "TF1Replay"),
                        Arguments.of("http://www.youtube.com/user/fakeUser", "Youtube")
                )
    }
}
