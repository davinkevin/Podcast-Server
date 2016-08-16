package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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
    @Mock
    UrlService urlService;
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
        when(jsonService.parse(anyString())).then(i -> JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build()).parse(i.getArgumentAt(0, String.class)));
    }

    @Test
    public void should_load_1080_stream() throws URISyntaxException, IOException, UnirestException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.1080p.html"));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);
        String anotherItemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-1920x1080/video/x4eq3z9.mp4?auth=1469777104-2562-rdf21ecr-a46fac6683a03618a720724db9f44b1c");
        assertThat(itemUrl).isSameAs(anotherItemUrl);
    }

    @Test
    public void should_load_720_stream() throws URISyntaxException, IOException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.720p.html"));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);
        String anotherItemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-1280x720/video/x4eq3z9.mp4?auth=1469777104-2562-b84nscso-712c4b4393d45fba34ae88084d27c46f");
        assertThat(itemUrl).isSameAs(anotherItemUrl);
    }

    @Test
    public void should_load_480_stream() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.480p.html"));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-848x480/video/x4eq3z9.mp4?auth=1469777104-2562-ekho4d17-c60c4162844bf079e16c2f6e84bd2b46");
    }

    @Test
    public void should_load_380_stream() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.380p.html"));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-512x384/video/x4eq3z9.mp4?auth=1469777104-2562-82qq5949-5ae5fd910db89335c537d448d29450e6");
    }

    @Test
    public void should_load_240_stream() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.240p.html"));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/H264-320x240/video/x4eq3z9.mp4?auth=1469777104-2562-y77qb97c-54cb20972eefbd1bd96eaccc0f17400a");
    }

    @Test
    public void should_load_auto() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.auto.html"));

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://www.dailymotion.com/cdn/manifest/video/x4eq3z9.m3u8?auth=1469777104-2562-jcoeffc7-51407ef99b433264b032634411fed1b5");
    }

    @SuppressWarnings("unchecked")
    private GetRequest mockGetRequestWithStringResponse(String uri) throws UnirestException {
        GetRequest request = mock(GetRequest.class);
        HttpResponse response = (HttpResponse<String>) mock(HttpResponse.class);
        when(request.asString()).thenReturn(response);
        when(response.getBody()).then(i -> IOUtils.fileAsString(uri));
        return request;
    }
}