package lan.dk.podcastserver.business;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.CoverAssert;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.UrlService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.util.FileSystemUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 24/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class CoverBusinessTest {

    String ROOT_FOLDER = "/tmp/podcast";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089); // No-args constructor defaults to port 8080

    @Before
    public void beforeEach() {
        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER).toFile());
    }

    @Mock CoverRepository coverRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Spy  UrlService urlService;
    @InjectMocks CoverBusiness coverBusiness;

    @Test
    public void should_find_one() {
        /* Given */
        when(coverRepository.findOne(any(UUID.class))).then(invocation -> new Cover().setId((UUID) invocation.getArguments()[0]));
        UUID id = UUID.randomUUID();
        /* When */
        Cover cover = coverBusiness.findOne(id);
        /* Then */  CoverAssert.assertThat(cover).hasId(id);
        verify(coverRepository, times(1)).findOne(eq(id));
    }

    @Test
    public void should_say_it_same_cover() {
        /* Given */ Podcast p1 = new Podcast(), p2 = new Podcast();
        p1.setCover(new Cover("http://fakeUrl.com"));
        p2.setCover(new Cover("http://fakeurl.com"));

        /* When */
        Boolean isSame = coverBusiness.hasSameCoverURL(p1, p2);
        /* Then */ assertThat(isSame).isTrue();
    }

    @Test
    public void should_say_its_not_same_cover() {
        /* Given */ Podcast p1 = new Podcast(), p2 = new Podcast();
        p1.setCover(new Cover("http://fakeUrl.com"));
        p2.setCover(new Cover("https://fakeurl.com"));

        /* When */
        Boolean isSame = coverBusiness.hasSameCoverURL(p1, p2);
        /* Then */ assertThat(isSame).isFalse();
    }

    @Test
    public void should_not_treat_this_podcast() {
        /* Given */ Podcast podcast = new Podcast();
        /* When */  String url = coverBusiness.download(podcast);
        /* Then */  assertThat(url).isEmpty();
    }

    @Test
    public void should_download_the_cover() throws URISyntaxException {
        /* Given */
        String podcastTitle = "Foo";
        String imageExtension = "png";
        String defaultCoverValue = "cover";
        String serverUrl = "http://localhost:8080/";
        Podcast podcast = new Podcast().setTitle(podcastTitle).setId(UUID.randomUUID());
        podcast.setCover(new Cover("http://localhost:8089/img/image." + imageExtension));
        when(podcastServerParameters.coverDefaultName()).thenReturn(defaultCoverValue);
        when(podcastServerParameters.rootFolder()).thenReturn(Paths.get(ROOT_FOLDER));
        when(podcastServerParameters.serverUrl()).thenReturn(new URI(serverUrl));
        /* When */
        String url = coverBusiness.download(podcast);

        /* Then */
        assertThat(url).isEqualTo(serverUrl + "api/podcast/" + podcast.getId() + "/" + defaultCoverValue + "." + imageExtension);
    }

    @Test
    public void should_reject_by_network_exception() throws URISyntaxException {
        /* Given */
        Podcast podcast = new Podcast().setTitle("Foo");
        podcast.setCover(new Cover("http://localhost:8089/img/image.jpg"));
        when(podcastServerParameters.coverDefaultName()).thenReturn("cover");
        when(podcastServerParameters.rootFolder()).thenReturn(Paths.get("/tmp"));
        when(podcastServerParameters.fileContainer()).thenReturn(new URI("http://localhost:8080/podcast"));
        /* When */
        String url = coverBusiness.download(podcast);

        /* Then */
        assertThat(url).isEmpty();
    }

    @Test
    public void should_generate_path_cover_of_podcast() {
        /* Given */
        Podcast podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("A Podcast")
                .cover(new Cover("http://podcast.dk.lan/podcast/Google/aCover.jpg"))
                .build();

        when(podcastServerParameters.coverDefaultName()).thenReturn("cover");
        when(podcastServerParameters.rootFolder()).thenReturn(Paths.get("/podcast/"));

        /* When */
        Path coverPathOf = coverBusiness.getCoverPathOf(podcast);

        /* Then */
        assertThat(coverPathOf.toString()).isEqualTo("/podcast/A Podcast/cover.jpg");
    }

    @Test
    public void should_save_cover() {
        /* Given */
        Cover cover = new Cover().setUrl("http://anUrl.jiz/foo/bar");
        UUID createdId = UUID.randomUUID();
        when(coverRepository.save(eq(cover))).thenReturn(cover.setId(createdId));

        /* When */
        Cover savedCover = coverBusiness.save(cover);

        /* Then */
        CoverAssert.assertThat(savedCover).hasId(createdId);
        assertThat(cover).isSameAs(savedCover);
        verify(coverRepository, only()).save(eq(cover));
    }
}