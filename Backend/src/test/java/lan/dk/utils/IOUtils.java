package lan.dk.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.vavr.control.Option;

import io.vavr.control.Try;
import io.vavr.jackson.datatype.VavrModule;
import org.apache.commons.codec.digest.DigestUtils;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static io.vavr.API.Option;
import static io.vavr.API.*;
/**
 * Created by kevin on 23/07/2016.
 */
public class IOUtils {

    private static final ParseContext PARSER = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider(
            new ObjectMapper()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .registerModules(
                            new Hibernate5Module()
                                    .enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING)
                                    .disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION),
                            new JavaTimeModule(),
                            new VavrModule()
                    )
    )).build());

    public static final String TEMPORARY_EXTENSION = ".psdownload";
    public static final Path ROOT_TEST_PATH = Paths.get("/tmp/podcast-server-test/");


    public static Try<Path> toPath(String uri) {
        return Try(() -> IOUtils.class.getResource(uri))
                .mapTry(URL::toURI)
                .map(Paths::get);
    }

    public static Option<org.jdom2.Document> fileAsXml(String uri) throws JDOMException, IOException, URISyntaxException {
        return toPath(uri)
                .map(Path::toFile)
                .mapTry(f -> new SAXBuilder().build(f))
                .toOption();
    }

    public static Option<Document> fileAsHtml(String uri) throws URISyntaxException, IOException {
        return toPath(uri)
                .map(Path::toFile)
                .mapTry(v -> Jsoup.parse(v, "UTF-8", ""))
                .toOption();
    }
    public static String fileAsString(String uri) {
        return toPath(uri)
                .mapTry(Files::newInputStream)
                .mapTry(org.apache.commons.io.IOUtils::toString)
                .getOrElseThrow(i -> new UncheckedIOException("Error during file fetching", IOException.class.cast(i)));
    }
    public static Option<DocumentContext> fileAsJson(String path) {
        return Option(path)
                .map(IOUtils.class::getResource)
                .toTry()
                .mapTry(URL::toURI)
                .map(Paths::get)
                .map(Path::toFile)
                .mapTry(PARSER::parse)
                .toOption();
    }
    public static InputStream fileAsStream(String file) {
        return IOUtils.class.getResourceAsStream(file);
    }
    public static BufferedReader fileAsReader(String file) {
        return toPath(file)
                .mapTry(Files::newBufferedReader)
                .getOrElseThrow(e -> new UncheckedIOException(new IOException("File " + file + " not found")));
    }

    public static InputStream urlAsStream(String url) {
        return Try(() -> new URL(url).openStream())
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
    }

    public static DocumentContext stringAsJson(String text) {
        return PARSER.parse(text);
    }
    public static Document stringAsHtml(String html) {
        return Try(() -> Jsoup.parse(html)).getOrElseThrow(e -> new RuntimeException("Error during conversion from string to html", e));
    }

    public static Path get(String uri) throws URISyntaxException {
        return Paths.get(IOUtils.class.getResource(uri).toURI());
    }
    public static String digest(String text) {
        return DigestUtils.md5Hex(text);
    }
}
