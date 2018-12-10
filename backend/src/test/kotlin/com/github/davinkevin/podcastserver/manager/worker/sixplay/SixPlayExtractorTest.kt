package com.github.davinkevin.podcastserver.manager.worker.sixplay

import arrow.core.None
import com.github.davinkevin.podcastserver.service.M3U8Service
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.request.GetRequest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.service.JsonService
import com.github.davinkevin.podcastserver.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*


/**
 * Created by kevin on 10/02/2018
 */
@ExtendWith(MockitoExtension::class)
class SixPlayExtractorTest {

    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var m3U8Service: M3U8Service
    @Mock lateinit var urlService: UrlService
    @InjectMocks lateinit var extractor: SixPlayExtractor

    private val itemClip = Item().apply { id = UUID.randomUUID(); url = "https://www.6play.fr/scenes-de-menages-p_829/episodes-du-09-fevrier-a-2025-c_11887179" }
    private val itemPlaylist = Item().apply { url = "https://www.6play.fr/scenes-de-menages-p_829/psychologie-du-couple-p_2372" }

    @Test
    fun `should extract real url for clip`() {
        /* GIVEN */
        whenever(jsonService.parseUrl(ITEM_6PLAY_URL)).thenReturn(IOUtils.fileAsJson(of("c_11887179.json")))
        whenever(urlService.getRealURL(ITEM_PHYSICAL_URL, UrlService.NO_OP, 0)).thenReturn(REAL_URL)

        val request = mock<GetRequest>()
        val response = mock<HttpResponse<String>>()
        whenever(urlService.get(REAL_URL)).thenReturn(request)
        whenever(request.header(any(), any())).thenReturn(request)
        whenever(request.asString()).thenReturn(response)
        whenever(response.rawBody).thenReturn(IOUtils.fileAsStream(of("118817179.manifest.m3u8")))

        whenever(m3U8Service.findBestQuality(any())).thenCallRealMethod()
        whenever(urlService.addDomainIfRelative(any(), any())).thenCallRealMethod()

        /* WHEN  */
        val downloadingItem = extractor.extract(itemClip)

        /* THEN  */
        assertThat(downloadingItem.item).isSameAs(itemClip)
        assertThat(downloadingItem.urls).containsOnly("https://cdn-m6web.akamaized.net/prime/vod/protected/d/a/6/Scenes-de-menages_c11887179_Episodes-du-09-fe/Scenes-de-menages_c11887179_Episodes-du-09-fe_sd3.mp4.m3u8")
    }

    @Test
    fun `should throw error due to unknown type of element for 6play`() {
        /* Given */
        val item = Item().apply { url = "https://www.6play.fr/scenes-de-menages-p_829/episodes-du-09-fevrier-a-2025-w_11887179" }
        /* When */
        assertThatThrownBy { extractor.extract(item) }
        /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Invalid type \"w\" for 6play item")
    }

    @Test
    fun `should throw error if no elements are found from the json result`() {
        /* GIVEN */
        whenever(jsonService.parseUrl(ITEM_6PLAY_URL)).then { None.toVΛVΓ() }

        /* WHEN  */
        assertThatThrownBy { extractor.extract(itemClip) }
        /* THEN  */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("No element founds for ${itemClip.id} at url ${itemClip.url}")
    }

    @Test
    fun `should extract urls from playlist`() {
        /* GIVEN */
        whenever(jsonService.parseUrl(PLAYLIST_6PLAY_URL)).thenReturn(IOUtils.fileAsJson(of("p_2372.json")))
        /* WHEN  */
        val downloadingItem = extractor.extract(itemPlaylist)
        /* THEN  */
        assertThat(downloadingItem.item).isSameAs(itemPlaylist)
        assertThat(downloadingItem.urls).containsOnly(
                // hq
                "https://lb.cdn.m6web.fr/p/s/5/db1b61589e0e02e10ceb781e66c5bad7/5a7f7550/u/videonum/4/8/8/scenesdemenages__POUVOIR-PSY__20170412__58edf15f83a5a_hq.mp4",

                // hd
                "https://lb.cdn.m6web.fr/p/s/5/155a99c7637201016c30311194321654/5a7f7550/u/videonum/2/5/2/scenesdemenages__ESTEVE-PSYCHOLOGUE__20170412__58edf33f9dcd7_hd.mp4",

                // sd
                "https://lb.cdn.m6web.fr/p/s/5/ebd00026549bd4ac9f8349442d55d3f4/5a7f7550/u/videonum/a/3/8/scenesdemenages__PSYCHOLOGIE__20170412__58edf19baa5a2_sd.mp4",

                // others in hq...
                "https://lb.cdn.m6web.fr/p/s/5/3ff48a5a18fc4f6572fec98aa0c0eba9/5a7f7550/u/videonum/3/3/c/scenesdemenages__PSY-CAUSE__20170412__58edf1d8d5cea_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/013e806c6252737caddd75702a0b842e/5a7f7550/u/videonum/d/5/2/scenesdemenages__PRETEXTE-PSY__20170412__58edf213ecb06_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/e2894d22c050a2d9adff246955af6fcd/5a7f7550/u/videonum/f/6/c/scenesdemenages__PSYTIF__20170412__58edf2500e8ab_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/0c8e3a159957d9a0049cc85695a25a15/5a7f7550/u/videonum/8/9/c/scenesdemenages__LHEURE-DU-PSY__20170412__58edf28b3a3f9_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/341d8d0e209fb4c05e6757a362eafa07/5a7f7550/u/videonum/d/2/4/scenesdemenages__RUBRIQUE-PSYCHO__20170412__58edf2c760ea9_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/805f0b0140725c6c7672a4bdd0e7822d/5a7f7550/u/videonum/2/d/6/scenesdemenages__PSYCHANALYSE__20170412__58edf30386411_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/514dbd763ad4e6225eab21cd5e84e99f/5a7f7550/u/videonum/5/9/e/scenesdemenages__DE-PSY-A-TREPAS__20170412__58edf340a0bf3_hq.mp4"
        )
    }

    @Test
    fun `should be only compatible with 6play url`() {
        assertThat(extractor.compatibility(null)).isGreaterThan(1)
        assertThat(extractor.compatibility("foo")).isGreaterThan(1)
        assertThat(extractor.compatibility("http://www.6play.fr/test")).isEqualTo(1)
    }


    companion object {
        private const val ITEM_PHYSICAL_URL = "https://lbv2.cdn.m6web.fr/v1/resource/s/usp/mb_sd3/d/a/6/Scenes-de-menages_c11887179_Episodes-du-09-fe/Scenes-de-menages_c11887179_Episodes-du-09-fe_unpnp.ism/Manifest.m3u8?expiration=1518300931&scheme=https&groups%5B0%5D=m6web&customerName=m6web&token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MTgyNjQ5MzAsIm5iZiI6MTUxODI2NDkzMCwiZXhwIjoxNTE4MzAwOTMxLCJyX2hhc2giOiJkN2I2M2ExMmRlMmVmOTYxY2Y4NTk2NjU1OTE0NWI0YjUxMmM2ZjFjIn0.lCbr5KE3dX6X7Ic3c8s9SYsJiZcDCYszmo-cGJYoG28"
        private const val REAL_URL = "https://cdn-m6web.akamaized.net/prime/vod/protected/d/a/6/Scenes-de-menages_c11887179_Episodes-du-09-fe/Scenes-de-menages_c11887179_Episodes-du-09-fe_sd3.m3u8"
        private const val ITEM_6PLAY_URL = "https://pc.middleware.6play.fr/6play/v2/platforms/m6group_web/services/6play/videos/clip_11887179?with=clips&csa=5"
        private const val PLAYLIST_6PLAY_URL = "https://pc.middleware.6play.fr/6play/v2/platforms/m6group_web/services/6play/videos/playlist_2372?with=clips&csa=5"

        private fun of(filename: String): String {
            return "/remote/podcast/6play/$filename"
        }
    }

}