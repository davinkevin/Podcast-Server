package lan.dk.podcastserver.manager.worker.mycanal;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class MyCanalFinderTest {

    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @Mock JsonService jsonService;
    private @InjectMocks
    MyCanalFinder finder;

    @Test
    public void should_find_podcast() {
        /* Given */
        Cover cover = Cover.builder().url("https://thumb.canalplus.pro/http/unsafe/1920x665/top/secure-media.mycanal.fr/image/56/9/mycanal_cover_logotypee_1920x665.25569.jpg").width(200).height(200).build();
        when(imageService.getCoverFromURL("https://thumb.canalplus.pro/http/unsafe/1920x665/top/secure-media.mycanal.fr/image/56/9/mycanal_cover_logotypee_1920x665.25569.jpg")).thenReturn(cover);
        when(htmlService.get("https://www.mycanal.fr/emissions/pid1319-le-tube.html")).thenReturn(IOUtils.fileAsHtml("/remote/podcast/mycanal/le-tube.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgument(0)));

        /* When */
        Podcast podcast = finder.find("https://www.mycanal.fr/emissions/pid1319-le-tube.html");

        /* Then */
        assertThat(podcast)
                .hasUrl("https://www.mycanal.fr/theme/emissions/pid1319-le-tube.html")
                .hasTitle("Le Tube")
                .hasType("MyCanal")
                .hasCover(cover);
    }

    @Test
    public void should_find_podcast_without_landing_page() {
        /* Given */
        when(htmlService.get("https://www.mycanal.fr/theme/emissions/pid4936-j-1.html")).thenReturn(IOUtils.fileAsHtml("/remote/podcast/mycanal/j_plus_1.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgument(0)));

        /* When */
        Podcast podcast = finder.find("https://www.mycanal.fr/theme/emissions/pid4936-j-1.html");

        /* Then */
        assertThat(podcast)
                .hasUrl("https://www.mycanal.fr/theme/emissions/pid4936-j-1.html")
                .hasTitle("J+1")
                .hasType("MyCanal")
                .hasCover(Cover.DEFAULT_COVER);
    }

    @Test
    public void should_return_nothing_if_structure_has_change() {
        /* GIVEN */
        when(htmlService.get("https://www.mycanal.fr/emissions/pid1319-le-tube.html")).thenReturn(IOUtils.fileAsHtml("/remote/podcast/mycanal/le-tube_without-data.html"));
        /* When */
        Podcast podcast = finder.find("https://www.mycanal.fr/emissions/pid1319-le-tube.html");
        /* THEN  */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST);
    }

    @Test
    public void should_be_compatible() {
        assertThat(finder.compatibility("https://www.mycanal.fr/emissions/pid1319-le-tube.html")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(finder.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1);
    }

}
