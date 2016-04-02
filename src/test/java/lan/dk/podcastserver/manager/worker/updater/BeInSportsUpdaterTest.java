package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.SignatureService;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks BeInSportsUpdater beInSportsUpdater;
    private static final String PROTOCOL = "http:";
    private static final String BE_IN_SPORTS_DOMAIN = PROTOCOL + "//www.beinsports.com%s";
    Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast
                .builder()
                .id(UUID.randomUUID())
                .url("http://be.in.sports.com/url/fake")
                .title("aBeInSportPodcast")
                .items(Sets.newHashSet())
                .build();
    }

    @Test
    public void should_do_singature_of_podcast() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/beinsports/lexpresso.html"));
        when(signatureService.generateMD5Signature(anyString())).thenReturn("aSignature");

        /* When */
        String signature = beInSportsUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_do_reject_is_signature_fail() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(Optional.empty());

        /* When */
        String signature = beInSportsUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("");
    }

    @Test
    public void should_return_empty_item_list_if_error() throws IOException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = beInSportsUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }
    
    @Test
    public void should_get_all_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/beinsports/lexpresso.html"));
        when(imageService.getCoverFromURL(anyString())).then(i -> new Cover((String) i.getArguments()[0]));
        configureForAllPage("/remote/podcast/beinsports/lexpresso.html");

        /* When */
        Set<Item> items = beInSportsUpdater.getItems(podcast);

        /* Then */
        assertThat(items)
                .hasSize(18)
                .extracting(Item::getUrl)
                .contains("http://www.dailymotion.com/cdn/H264-1280x720/video/x3koiok.mp4?auth=1452229872-2562-uow8ceq7-06bf8b8452db2af3d75327c80674343f",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3ig0n0.mp4?auth=1452229874-2562-m54mtomd-028db93ca85b11baf15329dce6db03bc",
                        null,
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3kdk13.mp4?auth=1452229873-2562-7ga3u9eb-45b1e89c0f2934d5068165f71ca7fee3",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3iav4n.mp4?auth=1452229874-2562-g10bn61a-b9e74b052600f60c2a82191bd4e6b025",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3khe1b.mp4?auth=1452229872-2562-lvul65re-3c44380253b3b950515a2a4c58910c9d",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3i1zbt.mp4?auth=1452229875-2562-pgfarwjf-0d511936aa075802b9f8e58082b4bc74",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3ig3m1.mp4?auth=1452229874-2562-wwmgazi3-6e5001ae26b2cc08777d166459d7abac",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3hli2o.mp4?auth=1452229875-2562-j54pktnl-6393caeba874aa1419c0b961b23024b0",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3jnsir.mp4?auth=1452229874-2562-5tjj9n94-f0da732cb8e61267f38a4af26316c519",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3koj1b.mp4?auth=1452229871-2562-y7zrs6c4-81c7a7341797b3d29ca98726a939fb32",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3kdju2.mp4?auth=1452229873-2562-drycuxej-6b7d069ec5a336568f6c6b78050cae52",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3kbccc.mp4?auth=1452229873-2562-2m5qs1qy-c49311f6ed67907acbf2b1c2aebffcc1",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3kl91v.mp4?auth=1452229872-2562-4oyb8vrb-b262e4580abcc1786af175f134fce3e6",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3i6dn0.mp4?auth=1452229875-2562-8doginj4-a90b0fec7cb6a5cc653e82ec6a90dc75",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3jzyq5.mp4?auth=1452229873-2562-dlnowbth-fde745e58a633f1fc76b767b94cd7657",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3i6f7n.mp4?auth=1452229875-2562-mnrnreto-fc3fe9915d53a83fe936dad2d7e818ce",
                        "http://www.dailymotion.com/cdn/H264-1280x720/video/x3hlhbm.mp4?auth=1452229876-2562-rs14ifem-32308ab7d6dcd841dde69ade51217db1");
    }

    @Test
    public void should_return_default_item_if_exception() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/beinsports/lexpresso.html"));
        when(htmlService.get(not(eq(podcast.getUrl())))).thenReturn(Optional.empty());

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
    
    /*@Test
    public void should_download_each_page() throws IOException, URISyntaxException {
        *//*Given*//*
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/beinsports/lexpresso.html"));
        String baseUrl = "http://www.beinsports.com%s";
        Path folder = Paths.get(BeInSportsUpdaterTest.class.getResource("/remote/podcast/beinsports/").toURI());

        *//*When*//*
        for (Element article : htmlService.get(podcast.getUrl()).select("article")) {
            String articleUrl = article.select("a").first().attr("data-url");
            String url = String.format(baseUrl, articleUrl);
            String id = StringUtils.substringBeforeLast(url.replace("http://www.beinsports.com/france/_components/replay-main/", ""), "/");
            Path file = folder.resolve(id + ".html");
            Files.deleteIfExists(file);
            try(InputStream is = new URL(url).openStream()) {
                Files.copy(is, file);
            }
            String iFrameUrl = "http:" + Jsoup.parse(file.toFile(), "UTF-8", "http://www.beinsports.com").select("iframe").attr("src");
            String daylimotionPageName = StringUtils.substringAfterLast(iFrameUrl, "/") + ".html";
            Path fileDailyMotion = folder.resolve(daylimotionPageName);
            Files.deleteIfExists(fileDailyMotion);
            try(InputStream is = new URL(iFrameUrl).openStream()) {
                Files.copy(is, fileDailyMotion);
            }
        }
    }*/

    private Answer<Optional<Document>> readHtmlFromFile(String s) throws URISyntaxException {
        Path path = Paths.get(BeInSportsUpdaterTest.class.getResource(s).toURI());
        return i -> Optional.of(Jsoup.parse(path.toFile(), "UTF-8", "http://www.beinsports.com/"));
    }

    private Answer<Optional<Document>> readHtmlFromFile(Path s) throws URISyntaxException {
        return i -> Optional.of(Jsoup.parse(s.toFile(), "UTF-8", "http://www.beinsports.com/"));
    }

    private void configureHtmlServiceWith(String url, String file) throws IOException, URISyntaxException {
        Path htmlPage = Paths.get(BeInSportsUpdaterTest.class.getResource(file).toURI());
        String iFrameUrl = "http:" + Jsoup.parse(htmlPage.toFile(), "UTF-8", "http://www.beinsports.com").select("iframe").attr("src");
        Path dailymotionPage = htmlPage.getParent().resolve(StringUtils.substringAfterLast(iFrameUrl, "/") + ".html");

        when(htmlService.get(eq(url))).then(readHtmlFromFile(htmlPage));
        when(htmlService.get(eq(iFrameUrl))).then(readHtmlFromFile(dailymotionPage));
    }

    private void configureForAllPage(String file) throws URISyntaxException, IOException {
        Path path = Paths.get(BeInSportsUpdaterTest.class.getResource(file).toURI());
        Document page = Jsoup.parse(path.toFile(), "UTF-8", "http://www.beinsports.com/");
        page.select("article")
                .stream()
                .map(e -> e.select("a").first().attr("data-url"))
                .map(e -> String.format(BE_IN_SPORTS_DOMAIN, e))
                .forEach(url -> {
                    try {
                        String id = StringUtils.substringBeforeLast(url.replace("http://www.beinsports.com/france/_components/replay-main/", ""), "/");
                        configureHtmlServiceWith(url, "/remote/podcast/beinsports/" + id + ".html");
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                });
    }

}
