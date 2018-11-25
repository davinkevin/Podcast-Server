package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.*
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Condition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

/**
 * Created by kevin on 25/12/2017
 */
@ExtendWith(MockitoExtension::class)
class MyCanalUpdaterTest {

    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var updater: MyCanalUpdater

    private val podcast = Podcast().apply {
        id = UUID.randomUUID()
        url = "https://www.mycanal.fr/url/fake"
        title = "A MyCanal Podcast"
        items = mutableSetOf()
    }

    @Test
    fun `should sign podcast`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(fileAsHtml(from("le-tube.html")))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("6ca1b384c76f88ae24d6bfd423333333")
        verify(signatureService, only()).fromText(any())
    }

    @Test
    fun `should throw error if fetch operation is empty`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(None.toVΛVΓ())

        /* When */
        assertThatThrownBy { updater.signatureOf(podcast) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Error during signature of ${podcast.title} with url ${podcast.url}")
    }

    @Test
    fun `should get items from podcast`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(fileAsHtml(from("le-tube.html")))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        whenever(jsonService.parseUrl(any())).then { fileAsJson(withId(it)) }
        listOf("http://media.canal-plus.com/image/76/0/738760.jpg", "http://media.canal-plus.com/image/40/9/732409.jpg",
                "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_733871_H.jpg", "http://media.canal-plus.com/image/76/1/735761.jpg",
                "http://media.canal-plus.com/image/97/4/731974.png", "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_734608_H.jpg",
                "http://media.canal-plus.com/image/31/4/738314.jpg", "http://media.canal-plus.com/image/93/4/737934.jpg",
                "http://media.canal-plus.com/image/39/4/735394.jpg", "http://media.canal-plus.com/image/07/0/735070.jpg",
                "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_736856_H.jpg", "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_736309_H.jpg",
                "http://media.canal-plus.com/image/23/0/734230.jpg", "http://media.canal-plus.com/image/36/8/737368.jpg",
                "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_733381_H.jpg", "http://media.canal-plus.com/image/89/4/732894.jpg")
                .forEach { it -> doReturn(Cover().apply { url = it; height = 200; width = 200 }).whenever(imageService).getCoverFromURL(it) }

        /* When */
        val items = updater.findItems(podcast)

        /* Then */
        assertThat(items).hasSize(16).are(coherent())
    }

    @Test
    fun `should get items from podcasts with multi value return for details`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(fileAsHtml(from("l-info-du-vrai.html")))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        whenever(jsonService.parseUrl(any())).then { fileAsJson(withId(it)) }
        doAnswer { Cover().apply { url = it.getArgument(0); height = 200; width = 200 } }
                .whenever(imageService).getCoverFromURL(argWhere { it in imageUrls })

        /* When */
        val items = updater.findItems(podcast)

        /* Then */
        assertThat(items).hasSize(120).are(coherent())
    }

    @Test
    fun `should return empty set if fetch operation return no elements`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.findItems(podcast)

        /* Then */
        assertThat(items).isEmpty()

    }

    @Test
    fun `should have a type`() {
        assertThat(updater.type().key).isEqualTo("MyCanal")
        assertThat(updater.type().name).isEqualTo("MyCanal")
    }

    @Test
    fun `should be compatible`() {
        assertThat(updater.compatibility("https://www.mycanal.fr/emissions/pid1319-le-tube.html")).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(updater.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1)
    }

    companion object {

        private fun coherent(): Condition<Item> {
            return object : Condition<Item>() {
                override fun matches(value: Item): Boolean {
                    assertThat(value.url).isNotEmpty()
                    assertThat(value.cover).isNotNull()
                    assertThat(value.pubDate).isNotNull()
                    assertThat(value.title).isNotEmpty()
                    assertThat(value.description).isNotEmpty()
                    return true
                }
            }
        }

        private fun withId(i: InvocationOnMock) = from(
                i.getArgument<String>(0)
                        .replace("https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/", "")
                        .replace("?format=json", "")
        ) + ".json"

        private fun from(name: String) = "/remote/podcast/mycanal/$name"

        private val imageUrls = listOf(
                "http://media.canal-plus.com/image/73/4/738734.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/nip-nip-142383-640x360-qw81a.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/VIGNETTE_AUTO_737930_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141923-640x360-txvwq.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142456-640x360-nmq0a.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140745-640x360-vhrzq.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141213-640x360-cnv0s.jpg",
                "http://media.canal-plus.com/image/92/2/737922.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-143455-640x360-oexdv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142203-640x360-twj5b.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-143239-640x360-ulpyb.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141926-640x360-z2lhe.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738599_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-143555-640x360-sze2c.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-143383-640x360-d01hv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-143554-640x360-ewrvc.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141882-640x360-vu4xs.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/VIGNETTE_AUTO_737847_H.jpg",
                "http://media.canal-plus.com/image/30/6/738306.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140236-640x360-ymnce.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-139104-640x360-eejls.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140143-640x360-cjfvn.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-143454-640x360-t2dfv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140995-640x360-rhnvz.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/VIGNETTE_AUTO_737851_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-139589-640x360-rte3y.jpg",
                "http://media.canal-plus.com/image/17/5/738175.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141066-640x360-tjzns.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140150-640x360-dzqxc.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140235-640x360-cwgzr.jpg",
                "http://media.canal-plus.com/image/37/8/738378.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140594-640x360-ajroa.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-138844-640x360-txnsz.jpg",
                "http://media.canal-plus.com/image/16/7/738167.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142821-640x360-tnnnm.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142308-640x360-tmtkr.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738601_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-142307-640x360-yuhwo.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141922-640x360-cje3y.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141065-640x360-odrvu.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-142728-640x360-eehpm.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/infoduvrai-640x360-wuuzs.jpg",
                "http://media.canal-plus.com/image/43/4/738434.jpg",
                "http://media.canal-plus.com/image/05/8/738058.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140746-640x360-rmpiu.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-143046-640x360-s1dbb.jpg",
                "http://media.canal-plus.com/image/31/0/738310.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-138927-640x360-nkx2q.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142729-640x360-cm0yw.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142381-640x360-a2xmw.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-139660-640x360-zenvy.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-143384-640x360-bmltt.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141002-640x360-clzmn.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140676-640x360-whjrr.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142006-640x360-dmj5c.jpg",
                "http://media.canal-plus.com/image/06/3/738063.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738520_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141928-640x360-nxfln.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-142913-640x360-d0k5r.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141799-640x360-zlbtr.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140471-640x360-vvhhu.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738595_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140145-640x360-tnpiq.jpg",
                "http://media.canal-plus.com/image/43/7/738437.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-142819-640x360-ewtsa.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-139975-640x360-ctjus.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-139911-640x360-tmfia.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141426-640x360-ohltt.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140234-640x360-derfc.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-142004-640x360-ajdwc.jpg",
                "http://media.canal-plus.com/image/92/8/737928.jpg",
                "http://media.canal-plus.com/image/73/6/738736.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142724-640x360-ndjks.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141798-640x360-wdk3m.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141425-640x360-dfnpv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-143238-640x360-mklyv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/VIGNETTE_AUTO_738266_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140670-640x360-ttn4s.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140545-640x360-rvbvd.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/nip-nip-142386-640x360-c0rrz.jpg",
                "http://media.canal-plus.com/image/85/4/737854.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140076-640x360-u21hu.jpg",
                "http://media.canal-plus.com/image/37/5/738375.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-143376-640x360-qjhkb.jpg",
                "http://media.canal-plus.com/image/73/5/738735.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141328-640x360-vulxn.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-143048-640x360-bmh0z.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142915-640x360-ajfts.jpg",
                "http://media.canal-plus.com/image/06/1/738061.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140470-640x360-tgxpd.jpg",
                "http://media.canal-plus.com/image/85/3/737853.jpg",
                "http://media.canal-plus.com/image/36/3/738363.jpg",
                "http://media.canal-plus.com/image/16/5/738165.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738527_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-140466-640x360-u2u1u.jpg",
                "http://media.canal-plus.com/image/43/6/738436.jpg",
                "http://media.canal-plus.com/image/91/7/737917.jpg",
                "http://media.canal-plus.com/image/43/5/738435.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141642-640x360-rxk0d.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141332-640x360-rhhwr.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-142997-640x360-t0lcv.jpg",
                "http://media.canal-plus.com/image/30/7/738307.jpg",
                "http://media.canal-plus.com/image/51/0/737510.png",
                "http://media.canal-plus.com/wwwplus/image/6/91/1/nip-nip-141643-640x360-mk9ga.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-139018-640x360-bgnwv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_737619_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_737614_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140542-640x360-rkdhr.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_737610_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738524_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-142198-640x360-qtdqu.jpg",
                "http://media.canal-plus.com/image/92/0/737920.jpg",
                "http://media.canal-plus.com/image/21/6/738216.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738469_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-141880-640x360-ntnnv.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-140595-640x360-n3fyt.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738259_H.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/3/nip-nip-138614-640x360-vhbpw.jpg",
                "http://media.canal-plus.com/wwwplus/image/6/91/2/VIGNETTE_AUTO_738257_H.jpg"
        )
    }

}