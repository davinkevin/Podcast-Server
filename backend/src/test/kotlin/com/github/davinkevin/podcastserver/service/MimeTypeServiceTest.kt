package com.github.davinkevin.podcastserver.service

import arrow.core.Option
import arrow.core.getOrElse
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.IOUtils
import org.apache.tika.Tika
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.nio.file.Paths

/**
 * Created by kevin on 22/07/2018
 */
@ExtendWith(MockitoExtension::class)
class MimeTypeServiceTest {

    @Nested
    inner class Service {
        @Mock lateinit var tikaProbeContentType: TikaProbeContentType
        @InjectMocks lateinit var mimeTypeService: MimeTypeService

        @Test
        fun `should get mimeType if no extension`() {
            /* When */
            val mimeType = mimeTypeService.getMimeType("")
            /* Then */
            assertThat(mimeType).isEqualTo("application/octet-stream")
        }

        @Test
        fun `should get mimetype for known extension`() {
            /* When */
            val mimeType = mimeTypeService.getMimeType("webm")
            /* Then */
            assertThat(mimeType).isEqualTo("video/webm")
        }

        @Test
        fun `should get mimetype for unknown extension`() {
            /* When */
            val mimeType = mimeTypeService.getMimeType("txt")
            /* Then */
            assertThat(mimeType).isEqualTo("unknown/txt")
        }

        @Test
        fun `should get extension by mimeType`() {
            /* Given */
            val item = Item().setMimeType("audio/mp3")

            /* When */
            val extension = mimeTypeService.getExtension(item)
            /* Then */ assertThat(extension).isEqualTo(".mp3")
        }

        @Test
        fun `should get extension by Youtube`() {
            /* Given */
            val item = Item().setPodcast(Podcast().setType("Youtube")).setUrl("http://fake.com/foo/bar")

            /* When */
            val extension = mimeTypeService.getExtension(item)
            /* Then */ assertThat(extension).isEqualTo(".mp4")
        }

        @Test
        fun `should get extension by url`() {
            /* Given */
            val item = Item()
                    .setPodcast(Podcast().setType("Other"))
                    .setUrl("http://fake.com/foo/bar.mp4a")

            /* When */
            val extension = mimeTypeService.getExtension(item)
            /* Then */
            assertThat(extension).isEqualTo(".mp4a")
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        fun `should get mimeType with probeContentType`() {
            /* Given */
            val path = IOUtils.toPath("/__files/service/mimeTypeService/plain.text.txt")
                    .getOrElse { throw RuntimeException("Error when fetchin file") }
            whenever(tikaProbeContentType.probeContentType(path)).thenReturn(Option.just("text/plain"))
            whenever(tikaProbeContentType.probeContentType(Paths.get("foo"))).thenReturn(Option.just(""))
            /* When */
            val type = mimeTypeService.probeContentType(path)
            /* Then */
            assertThat(type).isEqualTo("text/plain")
        }

        @Test
        fun `should get mimeType with tika`() {
            /* Given */
            val file = Paths.get("/", "tmp", "foo")
            whenever(tikaProbeContentType.probeContentType(file)).thenReturn(Option.just("Foo/bar"))
            /* When */
            val type = mimeTypeService.probeContentType(file)
            /* Then */
            assertThat(type).isEqualTo("Foo/bar")
        }

        @Test
        fun `should find mimeType from inline map`() {
            /* Given */
            val file = Paths.get("/", "tmp", "foo")
            whenever(tikaProbeContentType.probeContentType(file)).thenReturn(Option.empty())
            /* When */
            val type = mimeTypeService.probeContentType(file)
            /* Then */
            assertThat(type).isEqualTo("application/octet-stream")
        }
    }

    @Nested
    inner class TikaHelper {

        @Mock lateinit var tika: Tika
        @InjectMocks lateinit var tikaProbeContentType: TikaProbeContentType

        val file = Paths.get("/", "tmp", "Bar")

        @Test
        fun `should detect content type`() {
            /* Given */
            whenever(tika.detect(file)).thenReturn("Foo/bar")
            /* When */
            val type = tikaProbeContentType.probeContentType(file)
                    .getOrElse { throw RuntimeException("Not Found") }
            /* Then */
            assertThat(type).isEqualToIgnoringCase("Foo/bar")
        }

        @Test
        fun `should reject because exception`() {
            /* Given */
            whenever(tika.detect(file)).then { throw RuntimeException("Simulated Error") }
            /* When */
            val type = tikaProbeContentType.probeContentType(file)
            /* Then */
            assertThat(type.isEmpty()).isTrue()
        }
    }
}