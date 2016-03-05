package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lan.dk.podcastserver.entity.*;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class JdomServiceTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock MimeTypeService mimeTypeService;
    @Mock UrlService urlService;
    @InjectMocks JdomService jdomService;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(); // No-args constructor defaults to port 8080

    @Before
    public void beforeEach() {
        Item.fileContainer = "http://localhost:8080/podcast";
    }

    @Test
    public void should_parse() throws MalformedURLException {
        /* Given */
        stubFor(get(urlEqualTo("/a/valid.xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/jdomService/valid.xml")));

        /* When */
        Optional<Document> document = jdomService.parse(new URL("http://localhost:8080/a/valid.xml"));

        /* Then */
        assertThat(document).isPresent();
    }

    @Test
    public void should_do_error_during_parse() throws MalformedURLException {
        /* Given */
        stubFor(get(urlEqualTo("/a/invalid.xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/jdomService/invalid.xml")));

        /* When */
        Optional<Document> document = jdomService.parse(new URL("http://localhost:8080/a/invalid.xml"));

        /* Then */
        assertThat(document).isEmpty();
    }
    
    /*@Test
    public void should_parse_by_url() throws IOException, JDOMException, URISyntaxException {
        *//* Given *//*
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlService.getConnection(anyString())).thenReturn(urlConnection);
        *//*doThrow(IOException.class).when(urlConnection).getInputStream();*//*
        when(urlConnection.getInputStream()).thenReturn(Files.newInputStream(Paths.get(JdomServiceTest.class.getResource("/remote/podcast/rss.lesGrandesGueules.xml").toURI())));

        *//* When *//*
        Document document = jdomService.parse("aUrl");

        *//* Then *//*
        assertThat(document)
                .isInstanceOf(Document.class);
        assertThat(document.getRootElement().getName())
                .isEqualTo("rss");
    }

    @Test
    public void should_reject_parsing() throws IOException, JDOMException, URISyntaxException {
        *//* Given *//*
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlService.getConnection(anyString())).thenReturn(urlConnection);
        doThrow(IOException.class).when(urlConnection).getInputStream();

        *//* When *//*
        jdomService.parse("aUrl");
    }*/

    @Test
    public void should_generate_xml_from_podcast_with_only_50_items() throws URISyntaxException, IOException {
        /* Given */
        when(podcastServerParameters.getServerUrl()).thenReturn("http://localhost");
        when(podcastServerParameters.rssDefaultNumberItem()).thenReturn(50L);

        Podcast podcast = Podcast.builder()
                .id(1234)
                .title("FakePodcast")
                .description("Loren ipsum")
                .hasToBeDeleted(true)
                .cover(new Cover().setHeight(200).setWidth(200).setUrl("http://fake.url/1234/cover.png"))
                .tags(Collections.singleton(new Tag().setName("Open-Source")))
                .signature("123456789")
                .lastUpdate(ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris")))
                .build();

        podcast
                .setItems(generateItems(podcast, 100));

        /* When */
        String xml = jdomService.podcastToXMLGeneric(podcast, true);

        /* Then */
        assertThat(xml).isXmlEqualToContentOf(Paths.get(JdomService.class.getResource("/xml/podcast.output.50.xml").toURI()).toFile());
    }

    @Test
    public void should_generate_xml_from_podcast_with_only_all_items() throws URISyntaxException, IOException {
        /* Given */
        when(podcastServerParameters.getServerUrl()).thenReturn("http://localhost");

        Podcast podcast = Podcast.builder()
                .id(1234)
                .title("FakePodcast")
                .description("Loren ipsum")
                .hasToBeDeleted(true)
                .cover(new Cover().setHeight(200).setWidth(200).setUrl("http://fake.url/1234/cover.png"))
                .tags(Collections.singleton(new Tag().setName("Open-Source")))
                .signature("123456789")
                .lastUpdate(ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris")))
                .build();

        podcast
                .setItems(generateItems(podcast, 100));

        /* When */
        String xml = jdomService.podcastToXMLGeneric(podcast, false);

        /* Then */
        assertThat(xml).isXmlEqualToContentOf(Paths.get(JdomService.class.getResource("/xml/podcast.output.100.xml").toURI()).toFile());
    }

    private Set<Item> generateItems(Podcast podcast, Integer limit) {
        return IntStream.range(0, limit)
                .mapToObj(i -> new Item()
                        .setId(i)
                        .setFileName("name" + (Integer) i)
                        .setCover(new Cover().setHeight(200).setWidth(200).setUrl("http://fake.url/1234/items/" + i + "/cover.png"))
                        .setUrl("http://fake.url/1234/items/" + i + "/item.mp4")
                        .setTitle("name" + (Integer) i)
                        .setDescription((Integer) i + " Loren Ipsum")
                        .setDownloadDate(ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris")))
                        .setLength(i * 1024L)
                        .setLocalUri("http://fake.url/1234/items/" + i + "/item.mp4")
                        .setMimeType("video/mp4")
                        .setPodcast(podcast)
                        .setPubdate(ZonedDateTime.of(2015, 9, 8, 7, 0, 0, 0, ZoneId.of("Europe/Paris")).minusDays(i))
                        .setStatus(Status.FINISH))
                .collect(toSet());
    }

}