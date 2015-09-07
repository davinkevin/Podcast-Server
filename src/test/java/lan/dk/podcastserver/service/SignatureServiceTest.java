package lan.dk.podcastserver.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 04/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SignatureServiceTest {

    @Mock UrlService urlService;
    @InjectMocks SignatureService signatureService;

    @Test
    public void should_generate_md5_from_stream() throws IOException {
        /* Given */
        String stringStream = "azertyuiopqsdfghjklmwxcvbn";
        URLConnection connection = mock(URLConnection.class);
        when(urlService.getConnection(anyString())).thenReturn(connection);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(stringStream.getBytes()));

        /* When */
        String s = signatureService.generateSignatureFromURL("");

        /* Then */
        assertThat(s).isEqualTo(DigestUtils.md5Hex(stringStream));
    }
    
    @Test
    public void should_return_empty_string_if_error_during_connection() throws IOException {
        /* Given */
        URLConnection connection = mock(URLConnection.class);
        doThrow(IOException.class).when(urlService).getConnection(anyString());

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