package com.github.davinkevin.podcastserver.update.updaters

import com.github.davinkevin.podcastserver.update.updaters.dailymotion.DailymotionUpdater
import com.github.davinkevin.podcastserver.update.updaters.francetv.FranceTvUpdater
import com.github.davinkevin.podcastserver.update.updaters.gulli.GulliUpdater
import com.github.davinkevin.podcastserver.update.updaters.mytf1.MyTf1Updater
import com.github.davinkevin.podcastserver.update.updaters.rss.RSSUpdater
import com.github.davinkevin.podcastserver.update.updaters.upload.UploadUpdater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByApiUpdater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByXmlUpdater
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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
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
    @Mock lateinit var rssUpdater: RSSUpdater
    @Mock lateinit var myTf1Updater: MyTf1Updater
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
                rssUpdater,
                myTf1Updater,
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
        val uTypes: List<Type> = setOf(
                dailymotionUpdater,
                franceTvUpdater,
                gulliUpdater,
                rssUpdater,
                myTf1Updater,
                youtubeByXmlUpdater,
                youtubeByApiUpdater
        )
                .map { it.type() }


        /* When */
        val types = updaterSelector.types()

        /* Then */
        assertThat(types)
                .containsAll(uTypes)
                .hasSize(7)
    }

    companion object {
        @JvmStatic
        fun urlToUpdater() =
                Stream.of(
                        Arguments.of("http://www.dailymotion.com/showname", "Dailymotion"),
                        Arguments.of("http://www.france.tv/show/for/dummies", "FranceTv"),
                        Arguments.of("http://replay.gulli.fr/showname", "Gulli"),
                        Arguments.of("http://www.link.to.rss/feeds", "RSS"),
                        Arguments.of("http://www.tf1.fr/title", "MyTF1"),
                        Arguments.of("http://www.youtube.com/user/fakeUser", "Youtube")
                )
    }
}
