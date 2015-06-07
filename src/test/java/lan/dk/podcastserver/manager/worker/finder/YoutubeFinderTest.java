package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.JdomService;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YoutubeFinderTest {

    @Mock JdomService jdomService;
    @InjectMocks YoutubeFinder youtubeFinder;

    @Test
    @Ignore
    public void should_find_information_about_a_youtube_podcast_with_his_url () throws JDOMException, IOException, FindPodcastNotFoundException {
        //Given
        when(jdomService.jdom2Parse(anyString()))
                .then(invocationOnMock -> new SAXBuilder().build(Paths.get(YoutubeFinderTest.class.getResource("/remote/podcast/cauetofficiel.xml").toURI()).toFile()));

        //When
        Podcast podcast = youtubeFinder.find("https://www.youtube.com/user/cauetofficiel");
        Cover cover = podcast.getCover();

        //Then
        assertThat(podcast.getTitle()).isEqualToIgnoringCase("Cauet");
        assertThat(podcast.getDescription()).contains("La chaîne officielle de Cauet");
        assertThat(cover).isNotNull();
        assertThat(cover.getUrl()).isEqualToIgnoringCase("http://yt3.ggpht.com/-83tzNbjW090/AAAAAAAAAAI/AAAAAAAAAAA/Vj6_1jPZOVc/s88-c-k-no/photo.jpg");
    }
}