package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.UrlService;
import org.apache.commons.io.FilenameUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RSSFinderTest {

    @Mock UrlService urlService;
    @Mock JdomService jdomService;
    @Mock ImageService imageService;
    @InjectMocks RSSFinder rssFinder;

    private static final String COVER_URL = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg";

    @Before
    public void beforeEach() throws JDOMException, IOException {
        //Given
        URL emptyUrl = new URL("http://a.fake.url.com/withEmpty.xml");

        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL(((String) i.getArguments()[0]))));
        when(jdomService.parse(eq(emptyUrl))).thenReturn(Optional.empty());
        when(jdomService.parse(not(eq(emptyUrl)))).then(i -> Optional.of(urlToFile(i.getArguments()[0].toString())));
        when(imageService.getCoverFromURL(eq(COVER_URL))).thenReturn(Cover.builder().url(COVER_URL).build());
    }

    private Document urlToFile(String url) throws JDOMException, IOException, URISyntaxException {
        return new SAXBuilder().build(Paths.get(RSSFinderTest.class.getResource(
                "/remote/podcast/" + FilenameUtils.getName(url)
        ).toURI())
        .toFile());
    }

    @Test
    public void should_find_information_about_an_rss_podcast_with_his_url () throws JDOMException, IOException, FindPodcastNotFoundException {
        //When
        Podcast podcast = rssFinder.find("http://a.fake.url.com/rss.lesGrandesGueules.xml");
        Cover cover = podcast.getCover();

        //Then
        assertThat(podcast.getTitle()).isEqualToIgnoringCase("Les Grandes Gueules du Sport");
        assertThat(podcast.getDescription()).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ");
        assertThat(cover).isNotNull();
        assertThat(cover.getUrl()).isEqualToIgnoringCase(COVER_URL);
    }

    @Test
    public void should_find_information_with_itunes_cover() throws FindPodcastNotFoundException, IOException, JDOMException {
        //When
        Podcast podcast = rssFinder.find("http://a.fake.url.com/rss.lesGrandesGueules.withItunesCover.xml");

        //Then
        assertThat(podcast.getCover()).isNotNull();
        assertThat(podcast.getCover().getUrl()).isEqualTo(COVER_URL);
    }

    @Test
    public void should_find_information_without_any_cover() throws FindPodcastNotFoundException, IOException, JDOMException {
        //When
        Podcast podcast = rssFinder.find("http://a.fake.url.com/rss.lesGrandesGueules.withoutAnyCover.xml");

        //Then
        assertThat(podcast.getCover()).isNull();
    }

    @Test
    public void should_reject_if_not_found() throws JDOMException, IOException, FindPodcastNotFoundException {
        /* When */
        Podcast podcast = rssFinder.find("http://a.fake.url.com/withEmpty.xml");

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST);
    }
}