package com.github.davinkevin.podcastserver.manager.worker.francetv

/**
 * Created by kevin on 24/12/2017
 */
//@ExtendWith(SpringExtension::class)
//class FranceTvExtractorTest {
//
//    @Autowired lateinit var jsonService: JsonService
//    @Autowired lateinit var htmlService: HtmlService
//    @Autowired lateinit var extractor: FranceTvExtractor
//
//    val item = Item().apply {
//        title = "Secrets d'histoire - Jeanne d'Arc, au nom de Dieu"
//        url = "https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"
//    }
//
//    @BeforeEach
//    fun beforeEach() {
//        Mockito.reset(htmlService, jsonService)
//    }
//
//    @Test
//    fun `should get url for given item`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(fileAsHtml(from("948775-immersion-dans-le-mystere-toutankhamon.html")))
//        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
//        whenever(jsonService.parseUrl(any())).then { fileAsJson(from("c59c33ea-507b-11e9-b0a1-000d3a2437a2.json")) }
//
//        /* WHEN  */
//        val downloadingItem = extractor.extract(item)
//
//        /* THEN  */
//        assertThat(downloadingItem.url().toList()).containsOnly("https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif/2019/S13/J3/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682844-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais")
//        assertThat(downloadingItem.item).isSameAs(item)
//        assertThat(downloadingItem.filename).isEqualTo("948775-immersion-dans-le-mystere-toutankhamon.mp4")
//        verify(htmlService, times(1)).get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html")
//    }
//
//
//    @Test
//    fun `should use m3u8 url as backup if no hsl stream`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(fileAsHtml(from("948775-immersion-dans-le-mystere-toutankhamon.html")))
//        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
//        whenever(jsonService.parseUrl(any())).then { fileAsJson(from("c59c33ea-507b-11e9-b0a1-000d3a2437a2_with_m3u8_download.json")) }
//
//        /* WHEN  */
//        val downloadingItem = extractor.extract(item)
//
//        /* THEN  */
//        assertThat(downloadingItem.url().toList()).containsOnly("https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif/2019/S13/J3/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682844-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8")
//        assertThat(downloadingItem.item).isSameAs(item)
//        assertThat(downloadingItem.filename).isEqualTo("948775-immersion-dans-le-mystere-toutankhamon.mp4")
//        verify(htmlService, times(1)).get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html")
//    }
//
//    @Test
//    fun `should use first m3u8 stream if hls_v5_os and m3u8-download are not found`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(fileAsHtml(from("948775-immersion-dans-le-mystere-toutankhamon.html")))
//        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
//        whenever(jsonService.parseUrl(any())).then { fileAsJson(from("c59c33ea-507b-11e9-b0a1-000d3a2437a2_with_m3u8_download.json")) }
//
//        /* WHEN  */
//        val downloadingItem = extractor.extract(item)
//
//        /* THEN  */
//        assertThat(downloadingItem.url().toList()).containsOnly("https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif/2019/S13/J3/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682844-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8")
//        assertThat(downloadingItem.item).isSameAs(item)
//        assertThat(downloadingItem.filename).isEqualTo("948775-immersion-dans-le-mystere-toutankhamon.mp4")
//        verify(htmlService, times(1)).get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html")
//    }
//
//    @Test
//    fun `should use not secure url if secured not found`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(fileAsHtml(from("948775-immersion-dans-le-mystere-toutankhamon.html")))
//        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
//        whenever(jsonService.parseUrl(any())).then { fileAsJson(from("c59c33ea-507b-11e9-b0a1-000d3a2437a2_without_secured.json")) }
//
//        /* WHEN  */
//        val downloadingItem = extractor.extract(item)
//
//        /* THEN  */
//        assertThat(downloadingItem.url().toList()).containsOnly("http://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif/2019/S13/J3/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682844-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais")
//        assertThat(downloadingItem.item).isSameAs(item)
//        assertThat(downloadingItem.filename).isEqualTo("948775-immersion-dans-le-mystere-toutankhamon.mp4")
//        verify(htmlService, times(1)).get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html")
//    }
//
//    @Test
//    fun `should throw exception if no url found`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(fileAsHtml(from("948775-immersion-dans-le-mystere-toutankhamon.html")))
//        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
//        whenever(jsonService.parseUrl(any())).then { fileAsJson(from("c59c33ea-507b-11e9-b0a1-000d3a2437a2_without_video.json")) }
//
//        /* WHEN  */
//        assertThatThrownBy { extractor.extract(item) }
//                /* THEN  */
//                .isInstanceOf(RuntimeException::class.java)
//                .hasMessageStartingWith("No video found in this FranceTvItem")
//    }
//
//    @Test
//    fun `should throw error because every videos are offline`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(fileAsHtml(from("948775-immersion-dans-le-mystere-toutankhamon.html")))
//        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
//        whenever(jsonService.parseUrl(any())).then { fileAsJson(from("c59c33ea-507b-11e9-b0a1-000d3a2437a2_offline.json")) }
//
//        /* WHEN  */
//        assertThatThrownBy { extractor.extract(item) }
//                /* THEN  */
//                .isInstanceOf(RuntimeException::class.java)
//                .hasMessageStartingWith("No video found in this FranceTvItem")
//    }
//
//    @Test
//    fun `should not find any elements because layout change`() {
//        /* GIVEN */
//        whenever(htmlService.get("https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
//                .thenReturn(None.toVΛVΓ())
//
//        /* WHEN  */
//        assertThatThrownBy { extractor.extract(item) }
//                /* THEN  */
//                .isInstanceOf(RuntimeException::class.java)
//                .hasMessageStartingWith("Error during extraction of FranceTV item")
//
//    }
//
//    @Test
//    fun `should be compatible`() {
//        /* GIVEN */
//        val url = "https://www.france.tv/foo/bar/toto"
//        /* WHEN  */
//        val compatibility = extractor.compatibility(url)
//        /* THEN  */
//        assertThat(compatibility).isEqualTo(1)
//    }
//
//    @Test
//    fun `should not be compatible`() {
//        /* GIVEN */
//        val url = "https://www.france2.tv/foo/bar/toto"
//        /* WHEN  */
//        val compatibility = extractor.compatibility(url)
//        /* THEN  */
//        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
//    }
//
//    @TestConfiguration
//    @Import(FranceTvExtractor::class)
//    class LocalTestConfiguration {
//        @Bean fun jsonService() = mock<JsonService>()
//        @Bean fun htmlService() = mock<HtmlService>()
//    }
//
//    companion object {
//
//        private const val REAL_URL = "https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais"
//
//        private fun from(s: String) = "/remote/podcast/francetv/$s"
//        private fun fromCatalog(id: String) = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=$id"
//    }
//
//}
