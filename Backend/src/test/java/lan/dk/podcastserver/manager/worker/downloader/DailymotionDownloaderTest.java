package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import org.apache.commons.io.IOUtils;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionDownloaderTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock UrlService urlService;
    @Mock WGetFactory wGetFactory;
    @Mock JsonService jsonService;
    @InjectMocks DailymotionDownloader dailymotionDownloader;

    Item item;
    Podcast podcast;

    @Before
    public void beforeEach() {
        item = new Item()
                .setTitle("Title")
                .setUrl("http://a.fake.url/with/file.mp4?param=1")
                .setStatus(Status.NOT_DOWNLOADED);
        podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("A Fake Podcast")
                .items(Sets.newHashSet())
                .build()
                .add(item);

        dailymotionDownloader.setItem(item);
        dailymotionDownloader.setItemDownloadManager(itemDownloadManager);
        when(jsonService.from(anyString())).then(i -> Optional.of(new JSONParser().parse((String) i.getArguments()[0])));
    }

    @Test
    public void should_load_720_stream() throws URISyntaxException, IOException {
        /* Given */
        when(urlService.getPageFromURL(eq(item.getUrl()))).thenReturn(Optional.of(getPageAsString()));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);
        String anotherItemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-1280x720/video/x3cl49c.mp4?auth=1456335408-2562-lag9r5i4-9c4793582fdae442e91e60ba6a5c05b1");
        assertThat(itemUrl).isSameAs(anotherItemUrl);
    }

    @Test
    public void should_load_480_stream() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.getPageFromURL(eq(item.getUrl())))
                .thenReturn(Optional.of(getPageAsString().replace("720", "721")));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-848x480/video/x3cl49c.mp4?auth=1456335408-2562-rnn6dm9f-f6ed38b9ff15e210f8a98b2ed86d08d4");
    }

    @Test
    public void should_load_380_stream() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.getPageFromURL(eq(item.getUrl())))
                .thenReturn(Optional.of(getPageAsString().replace("720", "721").replace("480", "481")));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-512x384/video/x3cl49c.mp4?auth=1456335408-2562-w5klv6d7-80315d34ee0cd211d2a2dd7257da8090");
    }

    @Test
    public void should_load_240_stream() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.getPageFromURL(eq(item.getUrl())))
                .thenReturn(Optional.of(getPageAsString()
                        .replace("720", "721")
                        .replace("480", "481")
                        .replace("380", "381")));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-320x240/video/x3cl49c.mp4?auth=1456335408-2562-uzisfuko-32bfda979a0b15ba7f35c7454bf8a04a");
    }

    @Test
    public void should_load_auto() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.getPageFromURL(eq(item.getUrl())))
                .thenReturn(Optional.of(getPageAsString()
                        .replace("720", "721")
                        .replace("480", "481")
                        .replace("380", "381")
                        .replace("240", "241")
                ));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/manifest/video/x3cl49c.m3u8?auth=1456335408-2562-6j7oefiw-f726b6030784120d73ff8561049d8000");
    }

    private String getPageAsString() throws IOException, URISyntaxException {
        return IOUtils.toString(Files.newInputStream(Paths.get(DailymotionDownloaderTest.class.getResource("/remote/downloader/dailymotion/karimdebbache.dailymotion.html").toURI())));
    }

}