package com.github.davinkevin.podcastserver.service

import com.nhaarman.mockitokotlin2.whenever
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doThrow
import org.mockito.junit.jupiter.MockitoExtension
import java.io.UncheckedIOException

@ExtendWith(MockitoExtension::class)
internal class SignatureServiceTest {

    @Mock lateinit var urlService: UrlService
    @InjectMocks lateinit var signatureService: SignatureService

    @AfterEach fun afterEach() = Mockito.reset(urlService)

    @Test
    fun `should generate md5 from stream`() {
        /* Given */
        val stringStream = "azertyuiopqsdfghjklmwxcvbn"
        whenever(urlService.asStream(anyString())).thenReturn(stringStream.byteInputStream())


        /* When */
        val s = signatureService.fromUrl("")

        /* Then */
        Assertions.assertThat(s).isEqualTo(DigestUtils.md5Hex(stringStream))
    }

    @Test
    fun `should return empty string if error during connection`() {
        /* Given */
        doThrow(UncheckedIOException::class.java).whenever(urlService).asStream(anyString())

        /* When */
        val s = signatureService.fromUrl("")

        /* Then */
        Assertions.assertThat(s).isEqualTo("")
    }

    @Test
    fun `should generate md5 from text`() {
        /* Given */
        val stringStream = "azertyuiopqsdfghjklmwxcvbn"
        /* When */
        val s = signatureService.fromText(stringStream)
        /* Then */
        Assertions.assertThat(s).isEqualTo(DigestUtils.md5Hex(stringStream))
    }
}