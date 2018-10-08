package com.github.davinkevin.podcastserver.manager.worker.gulli

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import javax.validation.Validator

/**
 * Created by kevin on 14/10/2016
 */
@ExtendWith(MockitoExtension::class)
class GulliUpdaterTest {

    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var validator: Validator
    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var gulliUpdater: GulliUpdater

    val podcast = Podcast().apply {
        url = "http://replay.gulli.fr/dessins-animes/Pokemon3"
        title = "Pokemon"
    }

    @Test
    fun `should get signature`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(fileAsHtml(from("pokemon.html")))
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* When */
        val signature = gulliUpdater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("4d0bb11a29d851eabf10245b00d4cabe")
    }

    @Test
    fun `should return empty string if error during signature`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val signature = gulliUpdater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("")
    }

    @Test
    fun `should return list of items`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(IOUtils.fileAsHtml(from("pokemon.html")))
        doReturn(fileAsHtml(from("VOD68526621555000.html"))).whenever(htmlService).get("http://replay.gulli.fr/dessins-animes/Pokemon3/VOD68526621555000")
        doReturn(fileAsHtml(from("VOD68526621609000.html"))).whenever(htmlService).get("http://replay.gulli.fr/dessins-animes/Pokemon3/VOD68526621609000")
        whenever(imageService.getCoverFromURL(any())).then { Cover().apply { url = it.getArgument(0); height = 200; width = 200 } }
        /* When */
        val items = gulliUpdater.getItems(podcast)
        val first = items.filter { it.title.contains("13") }.getOrElseThrow { RuntimeException("Episode 13 Not Found") }
        val second = items.filter { it.title.contains("14") }.getOrElseThrow { RuntimeException("Episode 14 Not Found") }

        /* Then */
        assertThat(items).isNotEmpty.hasSize(2)

        assertThat(first.title).isEqualTo("Saison 19, Episode 13 : Voyages croisés")
        assertThat(first.url).isEqualTo("http://replay.gulli.fr/jwplayer/embed/VOD68526621555000")
        assertThat(first.description).isEqualTo("Sacha et Liam livrent un combat, et Amphinobi et Jungko, maintenant évolués, sont passionnés par cette revanche ! Non loin de là, Alain, qui est en voyage pour étudier la Méga-Évolution, s'arrête pour regarder le combat, et il est très intrigué de voir qu'Amphinobi semble se transformer, juste avant de vaincre Jungko. Alain veut en savoir davantage sur les possibilités de l'Amphinobi de Sacha, et il le met au défi de combattre son Dracaufeu. Lorsque Dracaufeu méga-évolue et qu'Amphinobi change à nouveau d'apparence, le combat devient intense ! Amphinobi finit par être vaincu mais tous souhaitent en savoir plus sur cette mystérieuse transformation ! Pendant ce temps, les expériences de la Team Flare semblent progresser...")
        assertThat(first.cover.url).isEqualTo("http://resize1-gulli.ladmedia.fr/r/280,210,smartcrop,center-top/img/var/storage/imports/replay/images/custom/thumbnails/snapshot_VOD68526621555000_20161007-141504.png")

        assertThat(second.title).isEqualTo("Saison 19, Episode 14 : Une opération explosive")
        assertThat(second.url).isEqualTo("http://replay.gulli.fr/jwplayer/embed/VOD68526621609000")
        assertThat(second.description).isEqualTo("À la recherche de la Team Flare et de Pouic, la Team Rocket rencontre un Pokémon qui lui ressemble, celui que l'on connait sous le nom de Z2...et la Team Flare est sur ses traces ! Les deux équipes s'affrontent, et Z2 change plusieurs fois de ravisseurs jusqu'au moment où il fusionne avec de nombreuses Cellules pour devenir un Pokémon reptilien puissant et menaçant, dont les attaques bousculent ses ravisseurs ! Z2 prend l'avantage, mais Lysandre a appelé du renfort ! Avec l'aide d'Alain et de son Méga-Dracaufeu, la Team Flare affronte Z2 et l'enferme dans une cage. Pendant ce temps, au campement de nos héros, Clem câline un Pouic triste et inquiet, en essayant de comprendre ce qui ne va pas...")
        assertThat(second.cover.url).isEqualTo("http://resize1-gulli.ladmedia.fr/r/280,210,smartcrop,center-top/img/var/storage/imports/replay/images/custom/thumbnails/snapshot_VOD68526621609000_20161007-142635.png")
    }

    @Test
    fun `should return empty list if video selector is not found`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(IOUtils.fileAsHtml(from("pokemon.with-different-format.html")))
        /* When */
        val items = gulliUpdater.getItems(podcast)
        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should return empty list if page isn't available`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(None.toVΛVΓ())
        /* When */
        val items = gulliUpdater.getItems(podcast)
        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should handle item with different structure`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(IOUtils.fileAsHtml(from("pokemon.with-different-item-format.html")))
        doReturn(fileAsHtml(from("VOD68526621555000.html"))).whenever(htmlService).get("http://replay.gulli.fr/dessins-animes/Pokemon3/VOD68526621555000")
        whenever(imageService.getCoverFromURL(any())).then { Cover().apply { url = it.getArgument(0); height = 200; width = 200 } }
        /* When */
        val items = gulliUpdater.getItems(podcast)
        /* Then */
        assertThat(items).hasSize(1)
    }

    @Test
    fun `should handle item without cover`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(IOUtils.fileAsHtml(from("pokemon.without-cover.html")))
        doReturn(fileAsHtml(from("VOD68526621609000.html"))).whenever(htmlService).get("http://replay.gulli.fr/dessins-animes/Pokemon3/VOD68526621609000")
        whenever(imageService.getCoverFromURL(any())).thenReturn(null)
        /* When */
        val items = gulliUpdater.getItems(podcast)
        /* Then */
        assertThat(items).hasSize(1)
        assertThat(items.toList()[0].cover).isEqualTo(Cover.DEFAULT_COVER)
    }

    @Test
    fun `should return Gulli Type`() {
        assertThat(gulliUpdater.type().key()).isEqualTo("Gulli")
        assertThat(gulliUpdater.type().name()).isEqualTo("Gulli")
    }

    @Test
    fun `should be compatible`() {
        assertThat(gulliUpdater.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(gulliUpdater.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isEqualTo(Integer.MAX_VALUE)
    }

    companion object {
        fun from(s:String) = "/remote/podcast/gulli/$s"
    }
}
