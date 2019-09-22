package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*

/**
 * Created by kevin on 03/12/2017
 */
class PassThroughExtractorTest {

    private var extractor: PassThroughExtractor = PassThroughExtractor()

    @Test
    fun `should return item and its url as is`() {
        /* GIVEN */
        val item = DownloadingItem (
                id = UUID.randomUUID(),
                title = "any",
                status = Status.NOT_DOWNLOADED,
                url = URI("https://www.any.com/video/foo?param=1"),
                numberOfFail = 0,
                progression = 0,
                podcast = DownloadingItem.Podcast(
                        id = UUID.randomUUID(),
                        title = "anyPodcast"
                ),
                cover = DownloadingItem.Cover(
                        id = UUID.randomUUID(),
                        url = URI("https://anyDomain/anyPodcast/cover.jpg")
                )
        )

        /* WHEN  */
        val extractedValue = extractor.extract(item)

        /* THEN  */
        assertThat(extractedValue.item).isEqualTo(item)
        assertThat(extractedValue.urls).containsOnly(item.url.toASCIIString())
    }

    @Test
    fun `should return max compatibility minus one`() {
        assertThat(extractor.compatibility(URI("https://foo.bar.com/")))
                .isEqualTo(Integer.MAX_VALUE - 1)
    }

}
