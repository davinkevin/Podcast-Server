package com.github.davinkevin.podcastserver.find.finders.noop

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.test.StepVerifier
import java.net.URI

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
class NoOpFinderTest {

    private val finder = NoOpFinder()

    @Test
    fun `should find default information`() {
        StepVerifier.create(finder.findInformation("https://foo.bar.com/"))
                .expectSubscription()
                .expectNext(FindPodcastInformation(
                        title = "",
                        url = URI("https://foo.bar.com/"),
                        description = "",
                        type = "noop",
                        cover = null
                ))
                .verifyComplete()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://www.dailymotion.com/",
        "https://www.france2.tv/france-2/vu/",
        "https://www.foo.com/france-2/vu/",
        "https://itunes.apple.com/foo/bar",
        "https://podcasts.apple.com/foo/bar",
        "https://www.mycanal.fr/france-2/vu/",
        "https://www.6play.fr/france-2/vu/",
        "https://youtube.com/channel/foo",
        "https://youtube.com/user/foo",
        "https://gdata.youtube.com/feeds/api/playlists/foo",
        "https://www.6play.fr/france-2/vu/"
    ])
    fun `should be compatible with every at minimum level`(/* Given */ url: String) {
        assertThat(finder.compatibility(url))
                .isEqualTo(Int.MAX_VALUE)
    }
}
