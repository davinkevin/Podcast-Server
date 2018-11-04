package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.M3U8Service
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.UrlService.Companion.USER_AGENT_DESKTOP
import com.github.davinkevin.podcastserver.service.UrlService.Companion.USER_AGENT_KEY
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.request.GetRequest
import com.nhaarman.mockitokotlin2.*
import io.vavr.API
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.io.input.NullInputStream
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.net.HttpURLConnection
import java.util.function.Consumer

/**
 * Created by kevin on 12/12/2017
 */
@ExtendWith(MockitoExtension::class)
class TF1ReplayExtractorTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var m3U8Service: M3U8Service
    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var urlService: UrlService
    @InjectMocks lateinit var extractor: TF1ReplayExtractor

    val item = Item().apply { url = "http://www.tf1.fr/tf1/19h-live/videos/19h-live-20-juillet-2016.html" }
    val url = "http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314"
    val connectionIsMadeAsDesktop: (Consumer<HttpURLConnection>) -> Boolean = {
        val c = mock<HttpURLConnection>()
        it.accept(c)
        verify(c).setRequestProperty(USER_AGENT_KEY, USER_AGENT_DESKTOP)
        true
    }

    @Nested
    @DisplayName("should get")
    inner class ShouldGet {

        private val apiRequest = mock<GetRequest>()
        private val apiResponse = mock<HttpResponse<String>>()
        private val m3u8request = mock<GetRequest>()
        private val m3u8Response = mock<HttpResponse<String>>()

        @BeforeEach
        fun beforeEach() {
            whenever(apiRequest.header(any(), any())).thenReturn(apiRequest)
            whenever(apiRequest.asString()).thenReturn(apiResponse)
            whenever(apiResponse.body).thenReturn("{\n" +
                    "  \"hls\": \"http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\",\n" +
                    "  \"mpd\": \"http://das-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.mpd?vk=MTMzMTU2NDUubXBk&st=SFYRoQwZsQ-84qF0vnX6Sw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\"\n" +
                    "}")
            whenever(m3u8request.header(any(), any())).thenReturn(m3u8request)
            whenever(m3u8request.asString()).thenReturn(m3u8Response)
            whenever(m3u8Response.rawBody).thenReturn(NullInputStream(1L))

            whenever(urlService.getRealURL(any(), any(), any())).thenReturn(url)
        }

        @Nested
        @DisplayName("with default behavior")
        inner class WithDefaultBehavior {

            @BeforeEach
            fun beforeEach() {
                whenever(m3U8Service.findBestQuality(any())).thenReturn(API.Option("foo/bar/video.mp4"))
                whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
                whenever(urlService.addDomainIfRelative(any(), any())).thenCallRealMethod()
            }

            @Test
            fun `real url`() {
                /* Given */
                whenever(htmlService.get(item.url)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"))
                doAnswer { apiRequest }.whenever(urlService).get("http://www.wat.tv/get/webhtml/13184238")
                doAnswer { m3u8request }.whenever(urlService).get(url)

                /* When */
                val downloadingItem = extractor.extract(item)

                /* Then */
                assertThat(downloadingItem.url().toList()).contains("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/foo/bar/video.mp4")
                assertThat(downloadingItem.filename).isEqualToIgnoringCase("19h-live-20-juillet-2016.mp4")
                assertThat(downloadingItem.userAgent).isEqualToIgnoringCase("AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)")
            }

            @Test
            fun `add user agent for real url`() {
                /* Given */
                whenever(htmlService.get(item.url)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"))
                doAnswer { apiRequest }.whenever(urlService).get("http://www.wat.tv/get/webhtml/13184238")
                doAnswer { m3u8request }.whenever(urlService).get(url)

                /* When */
                val downloadingItem = extractor.extract(item)

                /* Then */
                assertThat(downloadingItem.url().toList()).contains("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/foo/bar/video.mp4")
                assertThat(downloadingItem.filename).isEqualToIgnoringCase("19h-live-20-juillet-2016.mp4")
                assertThat(downloadingItem.userAgent).isEqualToIgnoringCase("AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)")
                verify(urlService).getRealURL(any(), argWhere(connectionIsMadeAsDesktop), any())
            }

            @Test
            fun `url with short id`() {
                /* Given */
                whenever(htmlService.get(item.url)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.short-id.item.html"))
                doAnswer { apiRequest }.whenever(urlService).get("http://www.wat.tv/get/webhtml/3184238")
                doAnswer { m3u8request }.whenever(urlService).get(url)

                /* When */
                val itemUrl = extractor.extract(item)

                /* Then */
                assertThat(itemUrl.url().toList()).contains("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/foo/bar/video.mp4")
            }
        }

        @Nested
        @DisplayName("with fall back url")
        inner class WithFallBackUrl {
            @Test
            fun `fall back to default url`() {
                /* Given */
                whenever(htmlService.get(item.url)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"))
                doAnswer { throw RuntimeException("Error during fetch of $url") }.whenever(urlService).get("http://www.wat.tv/get/webhtml/13184238")
                doAnswer { m3u8request }.whenever(urlService).get("http://wat.tv/get/ipad/13184238.m3u8")
                whenever(urlService.addDomainIfRelative(any(), any())).thenCallRealMethod()
                whenever(m3U8Service.findBestQuality(any())).thenReturn(API.Option("foo/bar/video.mp4"))

                /* When */
                val downloadingItem = extractor.extract(item)

                /* Then */
                assertThat(downloadingItem.url().toList()).contains("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/foo/bar/video.mp4")
                assertThat(downloadingItem.filename).isEqualToIgnoringCase("19h-live-20-juillet-2016.mp4")
                assertThat(downloadingItem.userAgent).isEqualToIgnoringCase("AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)")
            }
        }

        @Nested
        @DisplayName("an error")
        inner class AnError {

            @BeforeEach
            fun beforeEach() {
                whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
            }

            @Test
            fun `when fetching m3u8 file`() {
                /* Given */
                whenever(urlService.addDomainIfRelative(any(), any())).thenCallRealMethod()
                whenever(m3U8Service.findBestQuality(any())).thenReturn(API.Option("foo/bar/video.mp4"))
                whenever(htmlService.get(item.url)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"))
                doAnswer { apiRequest }.whenever(urlService).get(url)
                doAnswer { throw RuntimeException("Error during `get` of `http://www.wat.tv/get/webhtml/13184238`") }
                        .whenever(urlService).get("http://wat.tv/get/ipad/13184238.m3u8")

                /* When */
                assertThatThrownBy { extractor.extract(item) }
                /* Then */
                        .isInstanceOf(RuntimeException::class.java)
                        .hasMessage("Request end up on error for http://wat.tv/get/ipad/13184238.m3u8")
            }

            @Test
            fun `when searching for best m3u8 format`() {
                /* Given */
                whenever(m3U8Service.findBestQuality(any())).thenReturn(None.toVΛVΓ())
                whenever(htmlService.get(item.url)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"))
                doAnswer { apiRequest }.whenever(urlService).get("http://www.wat.tv/get/webhtml/13184238")
                doAnswer { m3u8request }.whenever(urlService).get(url)

                /* When */
                assertThatThrownBy { extractor.extract(item) }
                /* Then */
                        .isInstanceOf(RuntimeException::class.java)
                        .hasMessage("No m3u8 url found in $url")
            }
        }
    }

    @Test
    fun `should throw error if url can't be extracted`() {
        /* Given */
        whenever(htmlService.get(item.url)).then { None.toVΛVΓ() }
        /* When */
        assertThatThrownBy { extractor.extract(item) }
        /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Url not extracted for ${item.url}")
    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "www.tf1.fr/tf1/19h-live/videos"
        /* When */
        val compatibility = extractor.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "www.tf1.com/foo/bar/videos"
        /* When */
        val compatibility = extractor.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }
}