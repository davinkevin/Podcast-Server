package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.HtmlService;
import org.jdom2.JDOMException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YoutubeFinderTest {

    @Mock HtmlService htmlService;
    @InjectMocks YoutubeFinder youtubeFinder;

    @Test
    public void should_find_information_about_a_youtube_podcast_with_his_url () throws JDOMException, IOException, FindPodcastNotFoundException, URISyntaxException {
        //Given
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.get()).thenReturn(
                Jsoup.parse(
                        Paths.get(YoutubeFinderTest.class.getResource("/remote/podcast/youtube.cauetofficiel.html").toURI()).toFile(),
                        "UTF-8"
                )
        );
        when(htmlService.connectWithDefault(any())).thenReturn(mockConnection);

        //When
        Podcast podcast = youtubeFinder.find("https://www.youtube.com/user/cauetofficiel");
        Cover cover = podcast.getCover();

        //Then
        assertThat(podcast)
                .hasTitle("Cauet")
                .hasDescription("La chaîne officielle de Cauet, c'est toujours plus de kiff et de partage ! Des vidéos exclusives de C'Cauet sur NRJ tous les soirs de 19h à 22h. Des défis in...");
        assertThat(cover)
                .isNotNull()
                .hasUrl("https://yt3.ggpht.com/-83tzNbjW090/AAAAAAAAAAI/AAAAAAAAAAA/Vj6_1jPZOVc/s100-c-k-no/photo.jpg");
    }

    @Test(expected = FindPodcastNotFoundException.class)
    public void should_not_find_podcast_for_this_url() throws IOException, FindPodcastNotFoundException {
        /* Given */
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.get()).thenThrow(new IOException());
        when(htmlService.connectWithDefault(any())).thenReturn(mockConnection);
        /* When */
        youtubeFinder.find("https://www.youtube.com/user/cauetofficiel");
        /* Then -> See @Test Exception*/
    }

    @Test
    public void should_set_default_value_for_information_not_found() throws JDOMException, IOException, FindPodcastNotFoundException, URISyntaxException {
        //Given
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.get()).thenReturn(
                Jsoup.parse(
                        Paths.get(YoutubeFinderTest.class.getResource("/remote/podcast/youtube.cauetofficiel.withoutDescAndCoverAndTitle.html").toURI()).toFile(),
                        "UTF-8"
                )
        );
        when(htmlService.connectWithDefault(any())).thenReturn(mockConnection);

        //When
        Podcast podcast = youtubeFinder.find("https://www.youtube.com/user/cauetofficiel");
        Cover cover = podcast.getCover();

        //Then
        assertThat(podcast)
                .hasTitle("")
                .hasDescription("");
        assertThat(cover)
                .isNotNull()
                .hasUrl(null);
    }
}