package lan.dk.podcastserver.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 04/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SignatureServiceTest {

    private @Mock UrlService urlService;
    private @InjectMocks SignatureService signatureService;

    @Test
    public void should_generate_md5_from_stream() throws IOException {
        /* Given */
        String stringStream = "azertyuiopqsdfghjklmwxcvbn";
        when(urlService.asReader(anyString())).thenReturn(new BufferedReader(new StringReader(stringStream)));

        /* When */
        String s = signatureService.generateSignatureFromURL("");

        /* Then */
        assertThat(s).isEqualTo(DigestUtils.md5Hex(stringStream));
    }
    
    @Test
    public void should_return_empty_string_if_error_during_connection() throws IOException {
        /* Given */
        doThrow(UncheckedIOException.class).when(urlService).asReader(anyString());

        /* When */
        String s = signatureService.generateSignatureFromURL("");

        /* Then */
        assertThat(s).isEqualTo("");
    }

    @Test
    public void should_generate_md5_from_text() {
        /* Given */
        String stringStream = "azertyuiopqsdfghjklmwxcvbn";
        /* When */
        String s = signatureService.generateMD5Signature(stringStream);
        /* Then */
        assertThat(s).isEqualTo(DigestUtils.md5Hex(stringStream));
    }
}
