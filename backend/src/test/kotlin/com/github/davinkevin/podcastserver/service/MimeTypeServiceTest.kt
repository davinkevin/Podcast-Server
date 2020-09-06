package com.github.davinkevin.podcastserver.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.apache.tika.Tika
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Paths

/**
 * Created by kevin on 22/07/2018
 */
@ExtendWith(SpringExtension::class)
class MimeTypeServiceTest(
        @Autowired val mimeTypeService: MimeTypeService,
        @Autowired val tika: Tika
) {

    @TestConfiguration
    class LocalTestConfiguration {

        @Bean
        fun tika() = mock<Tika>()

        @Bean
        fun mimeTypeService(tika: Tika): MimeTypeService = MimeTypeService(
                tika = tika,
                mimeMap = mapOf("foo" to "video/foo")
        )
    }

    @Nested
    @DisplayName("resolve extension from map")
    inner class ResolveExtensionFromMap {

        @Test
        fun `should not find in inlined map and fallback to octet-stream`() {
            /* Given */
            val file = Paths.get("/", "tmp", "foo")
            whenever(tika.detect(file)).thenReturn(null)
            /* When */
            val type = mimeTypeService.probeContentType(file)
            /* Then */
            assertThat(type).isEqualTo("application/octet-stream")
        }

        @Test
        fun `should get mimeType if no extension`() {
            /* Given */
            val path = Paths.get("foo")
            whenever(tika.detect(path)).thenReturn(null)
            /* When */
            val type = mimeTypeService.probeContentType(path)
            /* Then */
            assertThat(type).isEqualTo("application/octet-stream")
        }

        @Test
        fun `should get mimetype for known extension`() {
            /* Given */
            val path = Paths.get("foo.foo")
            whenever(tika.detect(path)).thenReturn(null)
            /* When */
            val type = mimeTypeService.probeContentType(path)
            /* Then */
            assertThat(type).isEqualTo("video/foo")
        }

        @Test
        fun `should get mimetype for unknown extension`() {
            /* Given */
            val path = Paths.get("foo.extra")
            whenever(tika.detect(path)).thenReturn(null)
            /* When */
            val type = mimeTypeService.probeContentType(path)
            /* Then */
            assertThat(type).isEqualTo("unknown/extra")
        }

    }

    @Test
    fun `should get mimeType with probeContentType`() {
        /* Given */
        val path = Paths.get(MimeTypeServiceTest::class.java.getResource("/__files/service/mimeTypeService/plain.text.txt")
                .toURI()) ?: error("Error when fetching file")

        whenever(tika.detect(path)).thenReturn("text/plain")
        whenever(tika.detect(Paths.get("foo"))).thenReturn("")
        /* When */
        val type = mimeTypeService.probeContentType(path)
        /* Then */
        assertThat(type).isEqualTo("text/plain")
    }

    @Test
    fun `should get mimeType with tika`() {
        /* Given */
        val file = Paths.get("/", "tmp", "foo")
        whenever(tika.detect(file)).thenReturn("Foo/bar")
        /* When */
        val type = mimeTypeService.probeContentType(file)
        /* Then */
        assertThat(type).isEqualTo("Foo/bar")
    }
}
