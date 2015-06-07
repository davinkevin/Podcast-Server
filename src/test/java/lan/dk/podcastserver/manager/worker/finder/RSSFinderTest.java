package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RSSFinderTest {
    
    @Mock JdomService jdomService;
    @InjectMocks RSSFinder rssFinder;
    
    @Test
    public void should_find_information_about_an_rss_podcast_with_his_url () throws JDOMException, IOException, FindPodcastNotFoundException {
        //Given
        when(jdomService.jdom2Parse(anyString()))
                .then(invocationOnMock -> new SAXBuilder().build(Paths.get(RSSFinderTest.class.getResource((String) invocationOnMock.getArguments()[0]).toURI()).toFile()));
        
        //When
        Podcast podcast = rssFinder.find("/remote/podcast/lesGrandesGueules.xml");
        Cover cover = podcast.getCover();
        
        //Then
        assertThat(podcast.getTitle()).isEqualToIgnoringCase("Les Grandes Gueules du Sport");
        assertThat(podcast.getDescription()).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ");
        assertThat(cover).isNotNull();
        assertThat(cover.getUrl()).isEqualToIgnoringCase("http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg");
    }

}