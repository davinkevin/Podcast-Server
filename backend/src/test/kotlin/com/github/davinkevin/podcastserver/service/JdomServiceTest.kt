package com.github.davinkevin.podcastserver.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.*
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URISyntaxException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 08/09/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class JdomServiceTest {

    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var mimeTypeService: MimeTypeService
    @Mock lateinit var urlService: UrlService
    @InjectMocks lateinit var jdomService: JdomService

    private val itemId = listOf(
            "789CEC3A-72CB-970A-264A-D5B4BF953183",
            "374EA8C6-2390-EAB6-51D4-D3F5A8F5E7B7",
            "5F6CDA11-24E1-1FC9-8494-17BC9140BC52",
            "19508A7E-3588-8999-BAF4-BAD1215E3905",
            "BA7AAE7E-EDED-B7A0-578F-87E40CC39946",
            "8C352463-83F7-C2AD-355D-5D5741B359D4",
            "0FADEB67-3024-9EB3-4DB5-8231B80FDA0F",
            "7CD43954-67E6-50C1-BC3C-FE84A01EB007",
            "C175E97C-83D9-E4E5-7C2D-8DB98F5624EB",
            "37E4DB3C-E1D2-7A40-B438-63DE026C7108",
            "28606EB1-DA7F-6BA2-20A3-6AAD64C6CA9D",
            "C24B4DD3-DBE6-76B2-22AA-580D2515184C",
            "6866C5CE-2DAF-F618-EBD4-18EFE49F7F83",
            "D935B07E-34A9-ECFF-7105-9B0E07804593",
            "8F717FAB-F672-A7D5-22AD-D961797B1571",
            "05F62DD4-E345-3EEB-3E31-5A8A50A85175",
            "A60CCE2C-1F9C-47BE-B06B-04065D96F551",
            "655E53DB-0123-76D7-EB46-24A11A20FAB9",
            "5642AD48-DB22-0C7E-32CA-865E83D58E48",
            "2C2C2AD9-5A84-807B-81AF-55378B534B71",
            "31B8DC7E-2776-EA51-337F-C1D6CC834D3C",
            "C61CED6B-B4B0-9DC4-8250-B2FC970C2347",
            "16965687-7681-A69F-E63F-D10CABA71289",
            "BF6D3EFA-249F-318F-DAED-C98B4691374F",
            "8921F70B-076C-C3A1-C4F9-575C4002B46C",
            "EAAF0622-7666-7C56-3900-397F7C28F9A3",
            "81B81969-9EE0-2096-7CAF-351CAE861FB2",
            "1274A1DC-2FBC-0F23-1A74-04ACFEB0A1C4",
            "0E6A7771-43A6-3AD8-0D32-CF310B51FC99",
            "49B6CD3E-B753-54A9-F8AA-2328B2CCC5EE",
            "B6A65D53-481A-2FA0-224A-1D2180E6A44B",
            "80F5216A-523F-F44C-1AAF-05E691B129A9",
            "F6EE0A71-4E72-145A-118B-CD50BB4F20E6",
            "F0815CD5-8BAE-F606-7A21-1D8D78A24890",
            "770FFB9C-B574-71E0-9A45-6D28C13EA6B3",
            "A3A854AD-14F7-4AF0-C8DB-F87B1B907BD1",
            "EDED32E2-1873-A6DF-ABF5-A807F4B952EF",
            "5A685870-5983-6EB3-C4BE-3E6C9A060344",
            "45C729EA-D07C-D432-903D-BA960F2C0138",
            "4E678F7A-8BDD-EDDB-D2F4-9889E8812E31",
            "8CA49DF0-74CF-46EA-AE40-31DCACC8343B",
            "BAF6400A-86B7-26A3-0D15-3F4114FB6480",
            "5F7FBD82-6193-C4A4-BE0C-A80D183EF71B",
            "E89DD657-E0E7-22C1-182A-15C08E27CF2A",
            "3ADA3D16-2532-23C0-A759-D40536971C84",
            "76E6E670-DB1B-EBFA-00AF-C265FE190D2F",
            "D4B4486D-9922-0803-9FD4-CECDFDADB404",
            "233015A5-5DC0-5F27-833B-443FDEE78814",
            "29E10DE5-7923-38F7-F344-D18EA3098E35",
            "D5A8B94E-F3F8-3139-AA01-1EE1B87C994B",
            "4E39FF2E-C38A-73E7-C149-80CE8FD04C30",
            "EB2D2D61-B866-6F1E-38FB-DC7B7765AC9B",
            "7A55E4C6-1292-7B88-7F5B-C298C122C080",
            "C0226B7C-5DCF-CADC-1A57-A587BFB6A01D",
            "E467ACEA-49B3-B1FD-E896-E36E9EDC0B27",
            "E79FD812-CC75-F6E4-FE28-662C7D5DD2AB",
            "AB018AC3-7AD1-2CE5-B2ED-2C1D31D5F81D",
            "AF733DBB-A729-2249-EAD2-6D54D19A6649",
            "33C6F564-4381-9A7F-90A4-AF170FB6C48B",
            "7892B60C-3853-BD01-523F-771C035DCA2B",
            "F60F9886-493A-1BBC-8309-D4C2059A7F16",
            "61DC083F-72DF-2BA7-617F-EB0836B9A2FD",
            "154B9A56-4837-47A8-BD9F-95460762CA2A",
            "B7429E88-9DAA-7414-70DD-401C80A91277",
            "1DD0FE7E-D68E-337A-CCF5-A0EA91B92DBB",
            "ACC012B1-52EF-3C3C-79C3-A8158FE91956",
            "3A661F7E-6B60-5C5E-FBFE-DBFC3A6DB929",
            "CEFF6A05-BB29-B6EA-FD72-91C4FDAA80C6",
            "C3EA327B-9B18-F6C3-2957-5A2C59DBAF7C",
            "C096FC76-2E2E-91B3-68C1-FCC0C3695103",
            "E73F450A-3E8A-B1D8-66F2-AAC2FD11F554",
            "EF03ABAE-71F6-5BDC-925B-FC133400EF83",
            "104D571B-B51E-A8ED-B467-3FA12799F2A8",
            "3FB96AD3-A7D9-D507-BC8B-7DCBCFC70276",
            "1F05E6A7-C7AB-B4D1-2EAB-4D96196E869C",
            "C435D004-1CAD-0593-3D9A-E68F84973A59",
            "9201E321-A8F7-4BD2-0681-FE2C63870E46",
            "09E90274-403C-5C90-1254-229620D3401C",
            "B20EAADC-B340-164C-BBFF-AA86106AA437",
            "C3303127-5051-7343-6611-8C2B46197A08",
            "C9F2B71E-C55C-3C39-796A-A839990A3391",
            "41607B69-36AE-8219-2849-8012214A51FD",
            "E05AB24E-3FBB-D34A-8EE7-E1BC4C72F773",
            "FB887C66-3DC0-494D-C098-BE92539D50FA",
            "65C633AE-9C92-96BF-F934-9B14CD7D179F",
            "2A7FA7C4-44B4-6F78-7CAF-A95107BCD426",
            "C7791472-F0F0-A856-7517-22F410131D8E",
            "E5FEE21A-B7D2-8D72-75B1-E2BCD07D17D5",
            "B974A2CF-1FAA-E6A0-106C-759CDAAF7E0F",
            "65FEB3E5-3927-D930-C9B7-F3675A985EE6",
            "55F7DE3F-D43B-CC07-6A59-5BCBB71AE693",
            "372A81E1-1C7D-0B1F-CF23-E19E4C7B56F8",
            "24F51D1F-1696-E68E-40C1-84034C2238F2",
            "AF22D788-B903-09EC-699C-CEB963535B26",
            "162DC965-3C11-1528-6DD0-5216CC8A8B29",
            "46E45CF6-E978-1F13-5D67-7358E1000A0E",
            "3B82BEE4-7ABE-6272-6861-8F26BE670055",
            "CA3B7E39-58FD-FE7D-524A-3D743AB1469D",
            "EDB9627B-F120-EE05-CAE6-1BA28F437D55",
            "62927825-88E7-B99B-338E-4847D8B4142F"
    )

    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8181))

    @BeforeEach
    fun beforeEach() {
        wireMockServer.start()
        WireMock.configureFor("localhost", 8181)
    }

    @AfterEach
    fun afterEach() = wireMockServer.stop()

    @Test
    fun `should parse`() {
        /* Given */
        val url = "http://localhost:8181/a/valid.xml"
        whenever(urlService.asStream(anyString())).then { i -> IOUtils.urlAsStream(i.getArgument(0)) }
        stubFor(get(urlEqualTo("/a/valid.xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/jdomService/valid.xml")))

        /* When */
        val document = jdomService.parse(url)

        /* Then */
        assertThat(document.isDefined).isTrue()
        verify(urlService, only()).asStream(url)
    }

    @Test
    fun `should generate xml from podcast with only 50 items`() {
        /* Given */
        whenever(podcastServerParameters.rssDefaultNumberItem).thenReturn(50L)
        val podcast = Podcast().apply {
            id = UUID.fromString("029d7820-b7e1-4c0f-a94f-235584ffb570")
            title = "FakePodcast"
            description = "Loren ipsum"
            hasToBeDeleted = true
            cover = Cover.builder().height(200).width(200).url("http://fake.url/1234/cover.png").build()
            tags = setOf(Tag().setName("Open-Source"))
            signature = "123456789"
            lastUpdate = ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris"))
            items = generateItems(this, 100)
        }


        /* When */
        val xml = jdomService.podcastToXMLGeneric(podcast, "http://localhost", true)

        /* Then */
        assertThat(xml).isXmlEqualToContentOf(IOUtils.get("/xml/podcast.output.50.xml").toFile())
    }

    @Test
    fun `should generate xml from podcast with only all items`() {
        /* Given */
        val podcast = Podcast().apply {
            id = UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")
            title = "FakePodcast"
            description = "Loren ipsum"
            hasToBeDeleted = true
            cover = Cover.builder().height(200).width(200).url("/1234/cover.png").build()
            tags = setOf(Tag().setName("Open-Source"))
            signature = "123456789"
            lastUpdate = ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris"))
            items = generateItems(this, 100)
        }

        /* When */
        val xml = jdomService.podcastToXMLGeneric(podcast, "http://localhost", false)

        /* Then */
        assertThat(xml).isXmlEqualToContentOf(IOUtils.get("/xml/podcast.output.100.xml").toFile())
    }

    @Test
    @Throws(URISyntaxException::class)
    fun `should generate xml from watch list`() {
        /* Given */
        val podcastOne = Podcast().apply {
            id = UUID.fromString("029d7820-b7e1-4c0f-a94f-235584ffb570")
            title = "FakePodcast_1"
            cover = Cover.builder().height(200).width(200).url("http://fake.url/1234/cover.png").build()
            signature = "123456789"
            lastUpdate = ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris"))
        }

        val podcastTwo = Podcast().apply {
            id = UUID.fromString("526d5187-0563-4c44-801f-3bea447a86ea")
            title = "FakePodcast_2"
            cover = Cover.builder().height(200).width(200).url("http://fake.url/4567/cover.png").build()
            signature = "987654321"
            lastUpdate = ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris"))
        }

        val watchList = WatchList().apply {
            id = UUID.fromString("c988babd-a2b1-4774-b8f8-fa4903dc3786")
            name = "A custom WatchList"
            items = generateItems(podcastOne, 10) + (generateItems(podcastTwo, 20))
        }

        /* When */
        val xml = jdomService.watchListToXml(watchList, "http://localhost")

        /* Then */
        assertThat(xml).isXmlEqualToContentOf(IOUtils.get("/xml/watchlist.output.xml").toFile())
    }

    @Test
    fun `should generate opml from podcasts`() {
        /* GIVEN */
        val foo = Podcast().apply {
            id = UUID.fromString("05F62DD4-E345-3EEB-3E31-5A8A50A85175")
            title = "Foo"
            description = "DescFoo"
        }
        val bar = Podcast().apply {
            id = UUID.fromString("C24B4DD3-DBE6-76B2-22AA-580D2515184C")
            title = "Bar"
            description = "DescBar"
        }
        val last = Podcast().apply {
            id = UUID.fromString("EDB9627B-F120-EE05-CAE6-1BA28F437D55")
            title = "last"
            description = "Desc Last"
        }

        /* WHEN  */
        val opml = jdomService.podcastsToOpml(listOf(foo, bar, last), "http://fake.url/")

        /* THEN  */
        assertThat(opml).isEqualToIgnoringWhitespace(
                """<?xml version="1.0" encoding="UTF-8"?>
                <opml version="2.0">
                  <head>
                    <title>Podcast-Server</title>
                  </head>
                  <body>
                    <outline text="Bar" description="DescBar" htmlUrl="http://fake.url//podcasts/c24b4dd3-dbe6-76b2-22aa-580d2515184c" title="Bar" type="rss" version="RSS2" xmlUrl="http://fake.url//api/podcasts/c24b4dd3-dbe6-76b2-22aa-580d2515184c/rss" />
                    <outline text="Foo" description="DescFoo" htmlUrl="http://fake.url//podcasts/05f62dd4-e345-3eeb-3e31-5a8a50a85175" title="Foo" type="rss" version="RSS2" xmlUrl="http://fake.url//api/podcasts/05f62dd4-e345-3eeb-3e31-5a8a50a85175/rss" />
                    <outline text="last" description="Desc Last" htmlUrl="http://fake.url//podcasts/edb9627b-f120-ee05-cae6-1ba28f437d55" title="last" type="rss" version="RSS2" xmlUrl="http://fake.url//api/podcasts/edb9627b-f120-ee05-cae6-1ba28f437d55/rss" />
                  </body>
                </opml>""")
    }

    private fun generateItems(p: Podcast, limit: Int): Set<Item> =
            (0 until limit)
                    .map {
                        Item().apply {
                            id = UUID.fromString(itemId[it])
                            fileName = "name$it"
                            cover = Cover().apply {
                                height = 200
                                width = 200
                                url = "http://fake.url/1234/items/" + itemId[it] + "/cover.png"
                            }
                            url = "http://fake.url/1234/items/" + itemId[it] + "/item.mp4"
                            title = "name$it"
                            description = "$it Loren Ipsum"
                            downloadDate = ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris"))
                            length = it * 1024L
                            localUri = "http://fake.url/1234/items/${itemId[it]}/item.mp4"
                            mimeType = "video/mp4"
                            podcast = p
                            pubDate = ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris")).minusDays(it.toLong())
                            status = Status.FINISH
                        }
                    }.toSet()
}
