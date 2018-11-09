package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.service.UrlService
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.repository.CoverRepository
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.FileSystemUtils
import java.net.URISyntaxException
import java.nio.file.Paths
import java.util.*

/**
 * Created by kevin on 24/07/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class CoverBusinessTest {

    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(PORT))

    @Mock lateinit var coverRepository: CoverRepository
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Spy lateinit var urlService: UrlService
    @InjectMocks lateinit var coverBusiness: CoverBusiness

    @BeforeEach
    fun beforeEach() {
        FileSystemUtils.deleteRecursively(ROOT_FOLDER.toFile())
        wireMockServer.start()
        WireMock.configureFor(PORT)
    }

    @AfterEach
    fun afterEach() = wireMockServer.stop()

    @Test
    fun `should find one`() {
        /* Given */
        whenever(coverRepository.findById(any())).then { Optional.of(Cover().apply { id = it.arguments[0] as UUID } ) }
        val id = UUID.randomUUID()
        /* When */
        val cover = coverBusiness.findOne(id)
        /* Then */
        assertThat(cover.id).isEqualTo(id)
        verify(coverRepository, times(1)).findById(id)
    }

    @Test
    fun `should say it same cover`() {
        /* Given */
        val p1 = Podcast().apply {
            cover = Cover().apply { url = "http://fakeUrl.com"  }
        }
        val p2 = Podcast().apply {
            cover = Cover().apply { url = "http://fakeurl.com"  }
        }

        /* When */
        val isSame = coverBusiness.hasSameCoverURL(p1, p2)

        /* Then */
        assertThat(isSame).isTrue()
    }

    @Test
    fun `should say its not same cover`() {
        /* Given */
        val p1 = Podcast().apply {
            cover = Cover().apply { url = "http://fakeUrl.com"  }
        }
        val p2 = Podcast().apply {
            cover = Cover().apply { url = "https://fakeurl.com"  }
        }

        /* When */
        val isSame = coverBusiness.hasSameCoverURL(p1, p2)

        /* Then */
        assertThat(isSame).isFalse()
    }

    @Test
    fun `should not download cover of this podcast due to lack of cover`() {
        /* Given */
        val podcast = Podcast()

        /* When */
        val url = coverBusiness.download(podcast)

        /* Then */
        assertThat(url).isEmpty()
    }

    @Test
    fun `should not download cover of this podcast due to empty cover url`() {
        /* Given */
        val podcast = Podcast().apply {
            cover = Cover()
        }

        /* When */
        val url = coverBusiness.download(podcast)

        /* Then */
        assertThat(url).isEmpty()
    }

    @Test
    fun `should not handle local url`() {
        /* Given */
        val podcast = Podcast().apply {
            cover = Cover().apply { url = "/url/relative" }
        }

        /* When */
        val url = coverBusiness.download(podcast)

        /* Then */
        assertThat(url).isEqualTo("/url/relative")
    }

    @Test
    fun `should download the cover of podcast`() {
        /* Given */
        val podcastTitle = "Foo"
        val imageExtension = "png"
        val defaultCoverValue = "cover"
        val podcast = Podcast().apply {
            title = podcastTitle
            id = UUID.randomUUID()
            cover = Cover().apply { url = host("/img/image.$imageExtension") }
        }

        whenever(podcastServerParameters.coverDefaultName).thenReturn(defaultCoverValue)
        whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_FOLDER)

        /* When */
        val url = coverBusiness.download(podcast)

        /* Then */
        assertThat(url)
                .isEqualTo("/api/podcasts/${podcast.id}/$defaultCoverValue.$imageExtension")
    }

    @Test
    fun `should reject by network exception`() {
        /* Given */
        val podcast = Podcast().apply {
            title = "Foo"
            cover = Cover().apply { url = host("/img/image.jpg") }
        }
        whenever(podcastServerParameters.coverDefaultName).thenReturn("cover")
        whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get("/tmp"))

        /* When */
        val url = coverBusiness.download(podcast)

        /* Then */
        assertThat(url).isEmpty()
    }

    @Test
    fun `should generate path cover of podcast`() {
        /* Given */
        val podcast = Podcast().apply {
                id = UUID.randomUUID()
                title = "A Podcast"
                cover = Cover().apply { url = "http://foo.bar.com/podcast/Google/aCover.jpg" }
        }

        whenever(podcastServerParameters.coverDefaultName).thenReturn("cover")
        whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get("/podcast/"))

        /* When */
        val coverPathOf = coverBusiness.getCoverPathOf(podcast)

        /* Then */
        assertThat(coverPathOf.toString()).isEqualTo("/podcast/A Podcast/cover.jpg")
    }

    @Test
    fun `should save cover`() {
        /* Given */
        val cover = Cover().apply { url = "http://anUrl.jiz/foo/bar" }
        val createdId = UUID.randomUUID()
        whenever(coverRepository.save<Cover>(eq(cover))).thenReturn(cover.toBuilder().id(createdId).build())

        /* When */
        val savedCover = coverBusiness.save(cover)

        /* Then */
        assertThat(savedCover.id).isEqualTo(createdId)
        verify(coverRepository, only()).save(cover)
    }

    @Test
    fun `should not download cover of item because lack of id`() {
        /* Given */
        val item = Item()

        /* When */
        val downloaded = coverBusiness.download(item)

        /* Then */
        assertThat(downloaded).isFalse()
    }

    @Test
    fun `should not download cover of item because lack of cover`() {
        /* Given */
        val item = Item().apply {
            podcast = Podcast()
            id = UUID.randomUUID()
        }

        /* When */
        val downloaded = coverBusiness.download(item)

        /* Then */
        assertThat(downloaded).isFalse()
    }

    @Test
    fun `should download cover of item`() {
        /* Given */
        val item = Item().apply {
                id = UUID.randomUUID()
                podcast = Podcast().apply { title = "FooPodcast" }
                cover = Cover().apply { url = host("/img/image.png") }
        }
        whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_FOLDER)

        /* When */
        val downloaded = coverBusiness.download(item)

        /* Then */
        assertThat(downloaded).isTrue()
    }

    @Test
    @Throws(URISyntaxException::class)
    fun `should reject by network exception for item`() {
        /* Given */
        val item = Item().apply {
            id = UUID.randomUUID()
            podcast = Podcast().apply { title = "FooPodcast" }
            cover = Cover().apply { url = host("/img/image.jpg") }
        }
        whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_FOLDER)

        /* When */
        val downloaded = coverBusiness.download(item)

        /* Then */
        assertThat(downloaded).isFalse()
    }

    @Test
    fun `should find cover path of item`() {
        /* Given */
        // @formatter:off
        val item = Item().apply {
                id = UUID.randomUUID()
                cover = Cover().apply { url = "http://www.foo.bar/image.png" }
                podcast = Podcast().apply { title = "FooBarPodcast" }
        }
        whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get("/tmp/"))

        /* When */
        val path = coverBusiness.getCoverPathOf(item)

        /* Then */
        assertThat(path)
                .contains(Paths.get("/tmp", "FooBarPodcast", "${item.id}.png"))
    }

    companion object {

        private val ROOT_FOLDER = Paths.get("/tmp/podcast")
        private const val PORT = 8089
        private const val HTTP_LOCALHOST = "http://localhost:$PORT"

        private fun host(path: String): String {
            return HTTP_LOCALHOST + path
        }
    }
}
