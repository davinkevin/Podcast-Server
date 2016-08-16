package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import javaslang.control.Option;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.stream.Collector;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 11/06/15 for HackerRank problem
 */
@RunWith(MockitoJUnitRunner.class)
public class HtmlServiceTest {

    @Spy UrlService urlService;
    @InjectMocks HtmlService htmlService;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8090); // No-args constructor defaults to port 8080

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    public static final String URL = "http://nowhere.anywhere";

    @Test

    public void should_reject_get_with_empty() {
        /* Given */
        /* When */
        Option<Document> document = htmlService.get("http://localhost:8090/page/foo.html");

        /* Then */
        assertThat(document).isEmpty();
    }

    @Test
    public void should_get_page() throws InterruptedException {
        /* Given */
        stubFor(get(urlEqualTo("/page/file.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/htmlService/jsoup.html")));

        /* When */
        Option<Document> document = htmlService.get("http://localhost:8090/page/file.html");

        /* Then */
        assertThat(document.isDefined()).isTrue();
        assertThat(document.map(d -> d.head().select("title").text())).contains("JSOUP Example");
    }

    @Test
    public void should_parse_string() {
        /* Given */
        String html = "<div></div>";
        /* When */
        Document document = htmlService.parse(html);
        /* Then */
        assertThat(document).isNotNull().isInstanceOf(Document.class);
    }

    @Test
    public void should_generate_a_collector_for_html_element() {
        /* Given */
        Collector<Element, Elements, Elements> collector = HtmlService.toElements();
        Element divWithFooBar = new Element(Tag.valueOf("div"), "foo-bar");
        Element span = new Element(Tag.valueOf("span"), "Text");

        /* When */
        Elements listOfElements = collector.supplier().get();
        collector.accumulator().accept(listOfElements, divWithFooBar);
        Elements cobinedElements = collector.combiner().apply(listOfElements, new Elements(span));

        /* Then */
        assertThat(cobinedElements)
                .hasSize(2).contains(divWithFooBar, span)
                .isInstanceOf(Elements.class);
    }

}