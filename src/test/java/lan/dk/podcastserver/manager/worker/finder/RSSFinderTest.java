package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RSSFinderTest {
    
    @Mock JdomService jdomService;
    @Mock ImageService imageService;
    @InjectMocks RSSFinder rssFinder;
    
    @Test
    public void should_find_information_about_an_rss_podcast_with_his_url () throws JDOMException, IOException, FindPodcastNotFoundException {
        //Given
        parseFromURI();
        String coverUrl = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg";
        when(imageService.getCoverFromURL(eq(coverUrl))).thenReturn(new Cover(coverUrl));
        
        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.xml");
        Cover cover = podcast.getCover();
        
        //Then
        assertThat(podcast.getTitle()).isEqualToIgnoringCase("Les Grandes Gueules du Sport");
        assertThat(podcast.getDescription()).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ");
        assertThat(cover).isNotNull();
        assertThat(cover.getUrl()).isEqualToIgnoringCase(coverUrl);
    }

    @Test
    public void should_find_information_but_cover_generate_exception() throws JDOMException, IOException, FindPodcastNotFoundException {
        //Given
        parseFromURI();
        when(imageService.getCoverFromURL(anyString())).thenThrow(new IOException());

        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.xml");

        //Then
        assertThat(podcast.getCover()).isNull();
    }

    @Test
    public void should_find_information_with_itunes_cover() throws FindPodcastNotFoundException, IOException, JDOMException {
        //Given
        String coverUrl = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg";
        parseFromURI();
        when(imageService.getCoverFromURL(eq(coverUrl))).thenReturn(new Cover(coverUrl));

        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.withItunesCover.xml");

        //Then
        assertThat(podcast.getCover()).isNotNull();
        assertThat(podcast.getCover().getUrl()).isEqualTo(coverUrl);
    }

    @Test
    public void should_find_information_without_any_cover() throws FindPodcastNotFoundException, IOException, JDOMException {
        //Given
        parseFromURI();

        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.withoutAnyCover.xml");

        //Then
        assertThat(podcast.getCover()).isNull();
    }

    @Test(expected = FindPodcastNotFoundException.class)
    public void should_reject_if_not_found() throws JDOMException, IOException, FindPodcastNotFoundException {
        /* Given */ when(jdomService.jdom2Parse(anyString())).thenThrow(new JDOMException());
        /* When */  rssFinder.find("/remote/podcast/rss.lesGrandesGueules.xml");
        /* Then -> see @Test */
    }

    @Test(expected = FindPodcastNotFoundException.class)
    public void should_reject_if_channel_not_found() throws JDOMException, IOException, FindPodcastNotFoundException {
        /* Given */ parseFromURI();
        /* When */  rssFinder.find("/remote/podcast/rss.lesGrandesGueules.withoutChannel.xml");
        /* Then -> see @Test */
    }

    private void parseFromURI() throws JDOMException, IOException {
        when(jdomService.jdom2Parse(anyString()))
                .then(invocationOnMock -> new SAXBuilder().build(Paths.get(RSSFinderTest.class.getResource((String) invocationOnMock.getArguments()[0]).toURI()).toFile()));
    }

}