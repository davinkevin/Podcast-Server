package lan.dk.podcastserver.manager.worker.finder;

import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.utils.IOUtils;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RSSFinderTest {

    @Mock JdomService jdomService;
    @Mock ImageService imageService;
    @InjectMocks RSSFinder rssFinder;

    private static final String COVER_URL = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg";

    @Before
    public void beforeEach() throws JDOMException, IOException {
        //Given
        String url = "/remote/podcast/withEmpty.xml";

        when(jdomService.parse(eq(url))).thenReturn(Option.none());
        when(jdomService.parse(not(eq(url)))).then(i -> IOUtils.fileAsXml(i.getArgumentAt(0, String.class)));
        when(imageService.getCoverFromURL(eq(COVER_URL))).thenReturn(Cover.builder().url(COVER_URL).build());
    }

    @Test
    public void should_find_information_about_an_rss_podcast_with_his_url () throws JDOMException, IOException {
        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.xml");
        Cover cover = podcast.getCover();

        //Then
        assertThat(podcast.getTitle()).isEqualToIgnoringCase("Les Grandes Gueules du Sport");
        assertThat(podcast.getDescription()).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ");
        assertThat(cover).isNotNull();
        assertThat(cover.getUrl()).isEqualToIgnoringCase(COVER_URL);
    }

    @Test
    public void should_find_information_with_itunes_cover() throws IOException, JDOMException {
        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.withItunesCover.xml");

        //Then
        assertThat(podcast.getCover()).isNotNull();
        assertThat(podcast.getCover().getUrl()).isEqualTo(COVER_URL);
    }

    @Test
    public void should_find_information_without_any_cover() throws IOException, JDOMException {
        //When
        Podcast podcast = rssFinder.find("/remote/podcast/rss.lesGrandesGueules.withoutAnyCover.xml");

        //Then
        assertThat(podcast.getCover()).isEqualTo(Cover.DEFAULT_COVER);
    }

    @Test
    public void should_reject_if_not_found() throws JDOMException, IOException {
        /* When */
        Podcast podcast = rssFinder.find("/remote/podcast/withEmpty.xml");

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST);
    }
}
