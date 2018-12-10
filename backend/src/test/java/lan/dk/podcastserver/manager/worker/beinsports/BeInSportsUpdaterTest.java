package lan.dk.podcastserver.manager.worker.beinsports;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import com.github.davinkevin.podcastserver.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.UUID;

import static io.vavr.API.None;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 05/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class BeInSportsUpdaterTest {

    private static final String FILE_LOCATION_BY_ID = "/remote/podcast/beinsports/%s.html";
    private static final String PROTOCOL = "http:";
    private static final String BE_IN_SPORTS_DOMAIN = PROTOCOL + "//www.beinsports.com%s";

    /*private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock Validator validator;*/
    private @Mock SignatureService signatureService;
    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @InjectMocks
    BeInSportsUpdater beInSportsUpdater;

    Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast
                .builder()
                .id(UUID.randomUUID())
                .url("http://be.in.sports.com/url/fake")
                .title("aBeInSportPodcast")
                .items(HashSet.<Item>empty().toJavaSet())
                .build();
    }

    @Test
    public void should_do_singature_of_podcast() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(i -> IOUtils.fileAsHtml("/remote/podcast/beinsports/lexpresso.html"));
        when(signatureService.fromText(anyString())).thenReturn("aSignature");

        /* When */
        String signature = beInSportsUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_do_reject_is_signature_fail() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(None());

        /* When */
        String signature = beInSportsUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("");
    }

    @Test
    public void should_return_empty_item_list_if_error() throws IOException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(None());

        /* When */
        Set<Item> items = beInSportsUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }
    
    @Test
    public void should_get_all_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(i1 -> IOUtils.fileAsHtml("/remote/podcast/beinsports/lexpresso.html"));
        when(imageService.getCoverFromURL(anyString())).then(i -> Cover.builder().url(i.getArgument(0)).build());
        configureForAllPage("/remote/podcast/beinsports/lexpresso.html");

        /* When */
        Set<Item> items = beInSportsUpdater.getItems(podcast);

        /* Then */
        assertThat(items)
                .hasSize(18)
                .extracting(Item::getUrl)
                .contains("http://www.dailymotion.com/video/k4y3yexksKsmgXeCSxe",
                        "http://www.dailymotion.com/video/k4kpsSUv6NeSyHey4vH",
                        "http://www.dailymotion.com/video/k6y8VM67Tk51cFevGLN",
                        "http://www.dailymotion.com/video/k6qbquEPuMTupmeCSED",
                        "http://www.dailymotion.com/video/k5UHUxaU1GuLaIeBu0v",
                        "http://www.dailymotion.com/video/k2sZKI5e6O5o7uen8sV",
                        "http://www.dailymotion.com/video/k4igqOiv1xAV0xeAJpE",
                        "http://www.dailymotion.com/video/kVHDDo8VfKRgJLen7sM",
                        "http://www.dailymotion.com/video/kisGwjQrHFzAyaeh8Pw",
                        "http://www.dailymotion.com/video/k3Sh0hibWLhDAdeAJtJ",
                        "http://www.dailymotion.com/video/k5PwfhTuTv77H2eletm",
                        "http://www.dailymotion.com/video/k7GA9iL1BTLsPTelf0f",
                        "http://www.dailymotion.com/video/k3gO65ZnEZSFuBekn69",
                        "http://www.dailymotion.com/video/k2WnfDcKPl0fwmeCeSn",
                        "http://www.dailymotion.com/video/k55Re7mtUjT7EOeh95e",
                        "http://www.dailymotion.com/video/k1ngc9lfzaL98Mem6V1",
                        "http://www.dailymotion.com/video/k15nUb4MzCk7jeeaxJ5",
                        "http://www.dailymotion.com/video/kfJS0lTK7rLQMMeAiC0");
    }

    @Test
    public void should_return_default_item_if_exception() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(i -> IOUtils.fileAsHtml("/remote/podcast/beinsports/lexpresso.html"));
        when(htmlService.get(not(eq(podcast.getUrl())))).thenReturn(None());

        /* When */
        Set<Item> items = beInSportsUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(1).containsOnly(Item.DEFAULT_ITEM);
    }
    
    @Test
    public void should_not_contains_if_item_url_is_null() {
        /* Given */
        Item anItem = new Item();

        /* When */
        Boolean contains = beInSportsUpdater.notIn(podcast).test(anItem);

        /* Then */
        assertThat(contains).isTrue();
    }
    
    @Test
    public void should_contains_item_with_url() {
        /* Given */
        Item item = new Item().setUrl("http://www.dailymotion.com/cdn/H264-1280x720/video/x3hlhbm.mp4?auth=1452229876-2562-rs14ifem-32308ab7d6dcd841dde69ade51217db1");
        podcast.add(new Item().setUrl("http://www.dailymotion.com/cdn/H264-1280x720/video/x3hlhbm.mp4"));

        /* When */
        Boolean contains = beInSportsUpdater.notIn(podcast).test(item);

        /* Then */
        assertThat(contains).isFalse();
    }

    @Test
    public void should_have_a_type() {
        assertThat(beInSportsUpdater.type().name()).isEqualTo("Be In Sports");
        assertThat(beInSportsUpdater.type().key()).isEqualTo("BeInSports");
    }

    private void configureHtmlServiceWith(String url, String id) throws IOException, URISyntaxException {
        String page = String.format(FILE_LOCATION_BY_ID, id);
        Path htmlPage = IOUtils.toPath(page).get();
        String iFrameUrl = "http:" + Jsoup.parse(htmlPage.toFile(), "UTF-8", "http://www.beinsports.com").select("iframe").attr("src");
        String iframeLocation = String.format(FILE_LOCATION_BY_ID, StringUtils.substringAfterLast(iFrameUrl, "/"));

        when(htmlService.get(eq(url))).thenReturn(IOUtils.fileAsHtml(page));
        when(htmlService.get(eq(iFrameUrl))).thenReturn(IOUtils.fileAsHtml(iframeLocation));
    }

    private void configureForAllPage(String file) throws URISyntaxException, IOException {
        Path path = IOUtils.toPath(file).get();
        Document page = Jsoup.parse(path.toFile(), "UTF-8", "http://www.beinsports.com/");
        page.select("article")
                .stream()
                .map(e -> e.select("a").first().attr("data-url"))
                .map(e -> String.format(BE_IN_SPORTS_DOMAIN, e))
                .forEach(url -> Try.run(() -> {
                    String id = StringUtils.substringBeforeLast(url.replace("http://www.beinsports.com/france/_components/replay-main/", ""), "/");
                    configureHtmlServiceWith(url, id);
                }));
    }

}
