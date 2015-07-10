package lan.dk.podcastserver.utils;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 06/07/15 for Podcast Server
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = { URLUtils.class })
@Ignore
public class URLUtilsTest {

    @Test
    public void should_get_filename_from_canal_url() {
        /* Given */ String url = "http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8";
        /* When */  String fileNameFromCanalPlusM3U8Url = URLUtils.getFileNameM3U8Url(url);
        /* Then */  assertThat(fileNameFromCanalPlusM3U8Url).isEqualTo("NIP_1960_1500k.mp4");
    }
    
    @Test
    public void should_get_filename_from_m3u8_url() {
        /* Given */ String url = "http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif_france-dom-tom/2014/S42/J6/110996985-20141018-?null=";
        /* When */  String fileNameFromCanalPlusM3U8Url = URLUtils.getFileNameM3U8Url(url);
        /* Then */  assertThat(fileNameFromCanalPlusM3U8Url).isEqualTo("110996985-20141018-.mp4");
    }

    @Test
    public void should_get_connection_with_timeout() throws IOException {
        /* Given */ String url = "http://www.google.fr";
        /* When */  URLConnection urlConnection = URLUtils.getStreamWithTimeOut(url, 5000);
        /* Then */
            assertThat(urlConnection.getConnectTimeout()).isEqualTo(5000);
            assertThat(urlConnection.getReadTimeout()).isEqualTo(5000);
    }

    @Test
    public void should_get_connection_with_default_timeout() throws IOException {
        /* Given */ String url = "http://www.google.fr";
        /* When */  URLConnection urlConnection = URLUtils.getStreamWithTimeOut(url);
        /* Then */
        assertThat(urlConnection.getConnectTimeout()).isEqualTo(10000);
        assertThat(urlConnection.getReadTimeout()).isEqualTo(10000);
    }

    @Test
    public void should_return_url_with_specific_domain() {
        /* Given */ String domain = "http://www.google.fr/", subdomain = "subdomain";
        /* When */  String result = URLUtils.urlWithDomain(domain, subdomain);
        /* Then */  assertThat(result).isEqualTo("http://www.google.fr/subdomain");
    }

    @Test
    public void should_return_url_with_specific_domain_with_a_complete_url_as_subdomain() {
        /* Given */ String domain = "http://tty/", subdomain = "http://www.google.fr/subdomain";
        /* When */  String result = URLUtils.urlWithDomain(domain, subdomain);
        /* Then */  assertThat(result).isEqualTo("http://www.google.fr/subdomain");
    }

    @Test
    public void should_get_buffered_reader_of_url() throws Exception {
        /* Given */
        URL url = PowerMockito.mock(URL.class);
        when(url.openStream()).thenReturn(new ByteArrayInputStream("12345678".getBytes("UTF-8")));

        PowerMockito
                .whenNew(URL.class)
                .withParameterTypes(String.class)
                .withArguments(Mockito.anyString())
                .thenReturn(url);

        /* When */ Reader readerFromURL = URLUtils.getReaderFromURL("http://my.fake.url/");
        /* Then */ assertThat(((BufferedReader) readerFromURL).readLine())
                .isNotNull()
                .isEqualTo("12345678");
    }
    
    @Test
    public void should_get_last_m3u8_url() throws Exception {
        /* Given */
        URL url = PowerMockito.mock(URL.class);
        when(url.openStream()).thenReturn(new ByteArrayInputStream(Files.readAllBytes(Paths.get(URLUtilsTest.class.getResource("/remote/podcast/canalplus.lepetitjournal.20150707.m3u8").toURI()))));

        PowerMockito
                .whenNew(URL.class)
                .withParameterTypes(String.class)
                .withArguments(Mockito.anyString())
                .thenReturn(url);

        /* When */  String lastUrl = URLUtils.getM3U8UrlFormMultiStreamFile("http://my.fake.url");
        /* Then */  assertThat(lastUrl).isEqualTo("http://us-cplus-aka.canal-plus.com/i/1507/02/nip_NIP_59957_,200k,400k,800k,1500k,.mp4.csmil/segment146_3_av.ts");
    }

    @Test
    public void should_return_null_if_exception() throws Exception {
        /* Given */
        URL url = PowerMockito.mock(URL.class);
        when(url.openStream()).thenThrow(new IOException());

        PowerMockito
                .whenNew(URL.class)
                .withParameterTypes(String.class)
                .withArguments(Mockito.anyString())
                .thenReturn(url);

        /* When */  String lastUrl = URLUtils.getM3U8UrlFormMultiStreamFile("http://my.fake.url");
        /* Then */  assertThat(lastUrl).isNull();
    }

    @Test
    public void should_return_null_if_url_is_null() {
        /* Given */ String url = null;
        /* When */  String lastUrl = URLUtils.getM3U8UrlFormMultiStreamFile(null);
        /* Then */  assertThat(lastUrl).isNull();
    }
}