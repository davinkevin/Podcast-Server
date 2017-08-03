package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lan.dk.podcastserver.entity.Cover;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static lan.dk.podcastserver.entity.CoverAssert.assertThat;
import static org.mockito.Mockito.doThrow;

/**
 * Created by kevin on 06/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ImageServiceTest {

    private static final int PORT = 8089;
    private static final String HTTP_LOCALHOST = "http://localhost:" + PORT;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT); // No-args constructor defaults to port 8080

    @Spy
    UrlService urlService;
    @InjectMocks ImageService imageService;

    @Test
    public void should_return_null_or_empty_if_no_url() throws IOException {
        /* Given */ String url = "";
        /* When */ Cover coverFromURL = imageService.getCoverFromURL(url);
        /* Then */ assertThat(coverFromURL).isEqualTo(Cover.DEFAULT_COVER);
    }


    @Test
    public void should_retrieve_information_about_image() throws IOException {
        /* Given */
        String imagePath = "/img/image.png";

        stubFor(get(urlEqualTo(imagePath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "image/png")
                        .withBodyFile("img/image.png")));

        /* When */  Cover cover = imageService.getCoverFromURL(HTTP_LOCALHOST + imagePath);

        /* Then */  assertThat(cover)
                .hasWidth(256)
                .hasHeight(300)
                .hasUrl(HTTP_LOCALHOST + imagePath);
    }
    
    @Test
    public void should_reject_input_stream() throws IOException {
        /* Given */ doThrow(new IOException()).when(urlService).asStream(HTTP_LOCALHOST + "/img/image.png");
        /* When */  Cover cover = imageService.getCoverFromURL(HTTP_LOCALHOST + "/img/image.png");
        /* Then */  assertThat(cover).isEqualTo(Cover.DEFAULT_COVER);
    }
    
    @Test
    public void should_throw_exception_if_url_not_valid() {
        assertThat(imageService.getCoverFromURL("blabla")).isEqualTo(Cover.DEFAULT_COVER);
    }
}
