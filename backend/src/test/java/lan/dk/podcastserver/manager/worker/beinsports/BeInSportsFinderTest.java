package lan.dk.podcastserver.manager.worker.beinsports;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 18/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class BeInSportsFinderTest {

    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks
    BeInSportsFinder beInSportsFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        Cover cover = Cover.builder().url("https://images.beinsports.com/_04REUK9dN14HyrE2659T4C9zxQ=/670x424/smart/302352-Capture.PNG").width(200).height(200).build();
        when(imageService.getCoverFromURL(eq("https://images.beinsports.com/_04REUK9dN14HyrE2659T4C9zxQ=/670x424/smart/302352-Capture.PNG"))).thenReturn(cover);
        when(htmlService.get(eq("http://www.beinsports.com/france/replay/lexpresso"))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/beinsports/lexpresso.html"));

        /* When */
        Podcast podcast = beInSportsFinder.find("http://www.beinsports.com/france/replay/lexpresso");

        /* Then */
        assertThat(podcast)
                .hasUrl("http://www.beinsports.com/france/replay/lexpresso")
                .hasTitle("L'Expresso")
                .hasType("BeInSports")
                .hasCover(cover);
    }

    @Test
    public void should_be_compatible() {
        assertThat(beInSportsFinder.compatibility("http://www.beinsports.com/france/replay/lexpresso")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(beInSportsFinder.compatibility("http://www.foo.com/bar/folder")).isGreaterThan(1);
    }
}
