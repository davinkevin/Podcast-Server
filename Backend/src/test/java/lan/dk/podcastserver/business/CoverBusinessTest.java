package lan.dk.podcastserver.business;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.util.FileSystemUtils;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 24/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class CoverBusinessTest {

    private String ROOT_FOLDER = "/tmp/podcast";

    @ClassRule public static WireMockClassRule wireMockRule = new WireMockClassRule(8089); // No-args constructor defaults to port 8080
    @Rule public WireMockClassRule instanceRule = wireMockRule;

    @Before
    public void beforeEach() {
        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER).toFile());
    }

    @Mock CoverRepository coverRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Spy
    UrlService urlService;
    @InjectMocks CoverBusiness coverBusiness;

    @Test
    public void should_find_one() {
        /* Given */
        when(coverRepository.findOne(any(UUID.class))).then(invocation -> new Cover().setId((UUID) invocation.getArguments()[0]));
        UUID id = UUID.randomUUID();
        /* When */
        Cover cover = coverBusiness.findOne(id);
        /* Then */
        assertThat(cover).hasId(id);
        verify(coverRepository, times(1)).findOne(eq(id));
    }

    @Test
    public void should_say_it_same_cover() {
        /* Given */ Podcast p1 = new Podcast(), p2 = new Podcast();
        p1.setCover(Cover.builder().url("http://fakeUrl.com").build());
        p2.setCover(Cover.builder().url("http://fakeurl.com").build());

        /* When */
        Boolean isSame = coverBusiness.hasSameCoverURL(p1, p2);
        /* Then */ assertThat(isSame).isTrue();
    }

    @Test
    public void should_say_its_not_same_cover() {
        /* Given */ Podcast p1 = new Podcast(), p2 = new Podcast();
        p1.setCover(Cover.builder().url("http://fakeUrl.com").build());
        p2.setCover(Cover.builder().url("https://fakeurl.com").build());

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
    public void should_not_handle_local_url() {
        /* Given */ Podcast podcast = Podcast.builder().cover(Cover.builder().url("/url/relative").build()).build();
        /* When */  String url = coverBusiness.download(podcast);
        /* Then */  assertThat(url).isEqualTo("/url/relative");
    }

    @Test
    public void should_download_the_cover_of_podcast() throws URISyntaxException {
        /* Given */
        String podcastTitle = "Foo";
        String imageExtension = "png";
        String defaultCoverValue = "cover";
        Podcast podcast = new Podcast().setTitle(podcastTitle).setId(UUID.randomUUID());
        podcast.setCover(Cover.builder().url("http://localhost:8089/img/image." + imageExtension).build());

        when(podcastServerParameters.getCoverDefaultName()).thenReturn(defaultCoverValue);
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get(ROOT_FOLDER));

        /* When */
        String url = coverBusiness.download(podcast);

        /* Then */
        assertThat(url).isEqualTo("/api/podcast/" + podcast.getId() + "/" + defaultCoverValue + "." + imageExtension);
    }

    @Test
    public void should_reject_by_network_exception() throws URISyntaxException {
        /* Given */
        Podcast podcast = new Podcast().setTitle("Foo");
        podcast.setCover(Cover.builder().url("http://localhost:8089/img/image.jpg").build());
        when(podcastServerParameters.getCoverDefaultName()).thenReturn("cover");
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/tmp"));

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
                .cover(Cover.builder().url("http://podcast.dk.lan/podcast/Google/aCover.jpg").build())
                .build();

        when(podcastServerParameters.getCoverDefaultName()).thenReturn("cover");
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/podcast/"));

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
        assertThat(savedCover).hasId(createdId);
        assertThat(cover).isSameAs(savedCover);
        verify(coverRepository, only()).save(eq(cover));
    }

    @Test
    public void should_not_download_cover_of_item_because_lack_of_id() {
        /* Given */
        Item item = Item.builder().build();

        /* When */
        Boolean downloaded = coverBusiness.download(item);

        /* Then */
        assertThat(downloaded).isFalse();
    }

    @Test
    public void should_not_download_cover_of_item_because_lack_of_cover() {
        /* Given */
        Item item = Item.builder().podcast(new Podcast()).id(UUID.randomUUID()).build();

        /* When */
        Boolean downloaded = coverBusiness.download(item);

        /* Then */
        assertThat(downloaded).isFalse();
    }

    @Test
    public void should_download_cover_of_item() {
        /* Given */
        Item item = Item.builder()
                .id(UUID.randomUUID())
                .podcast(Podcast.builder().title("FooPodcast").build())
                .cover(Cover.builder().url("http://localhost:8089/img/image.png").build())
                .build();
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get(ROOT_FOLDER));

        /* When */
        Boolean downloaded = coverBusiness.download(item);

        /* Then */
        assertThat(downloaded).isTrue();
    }

    @Test
    public void should_reject_by_network_exception_for_item() throws URISyntaxException {
        /* Given */
        Item item = Item.builder()
                    .id(UUID.randomUUID())
                    .podcast(Podcast.builder().title("FooPodcast").build())
                    .cover(Cover.builder().url("http://localhost:8089/img/image.jpg").build())
                .build();
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get(ROOT_FOLDER));

        /* When */
        Boolean downloaded = coverBusiness.download(item);

        /* Then */
        assertThat(downloaded).isFalse();
    }

    @Test
    public void should_find_cover_path_of_item() {
        /* Given */
        // @formatter:off
        Item item = Item
            .builder()
                .id(UUID.randomUUID())
                .cover(Cover.builder().url("http://www.foo.bar/image.png").build())
                .podcast(Podcast.builder().title("FooBarPodcast").build())
            .build();
        // @formatter:on
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/tmp/"));

        /* When */
        Path path = coverBusiness.getCoverPathOf(item);

        /* Then */
        assertThat(path).isEqualTo(Paths.get("/tmp", "FooBarPodcast", item.getId() + ".png"));
    }
}