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
import javaslang.control.Option;
import javaslang.control.Try;
import javaslang.jackson.datatype.JavaslangModule;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Jsoup;

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
                            new JavaslangModule()
                    )
    )).build());

    public static Option<org.jdom2.Document> fileAsXml(String path) throws JDOMException, IOException, URISyntaxException {
        return Option.of(new SAXBuilder().build(Paths.get(IOUtils.class.getResource(path).toURI()).toFile()));
    }
    public static Option<org.jsoup.nodes.Document> fileAsHtml(String path) throws URISyntaxException, IOException {
        return Option.of(Jsoup.parse(Paths.get(IOUtils.class.getResource(path).toURI()).toFile(), "UTF-8", ""));
    }
    public static String fileAsString(String uri) {
        return Try.of(() -> IOUtils.class.getResource(uri))
                .mapTry(URL::toURI)
                .map(Paths::get)
                .mapTry(Files::newInputStream)
                .mapTry(org.apache.commons.io.IOUtils::toString)
                .getOrElseThrow(i -> new UncheckedIOException("Error during file fetching", IOException.class.cast(i)));
    }
    public static Option<DocumentContext> fileAsJson(String path) {
        return Option.of(path)
                .map(IOUtils.class::getResource)
                .toTry()
                .mapTry(URL::toURI)
                .map(Paths::get)
                .map(Path::toFile)
                .mapTry(PARSER::parse)
                .toOption();
    }
    public static DocumentContext parseJson(String json) {
        return PARSER.parse(json);
    }

    public static InputStream fileAsStream(String file) {
        return IOUtils.class.getResourceAsStream(file);
    }
    public static BufferedReader fileAsReader(String file) {
        return Try.of(() -> IOUtils.class.getResource(file).toURI())
                .map(Paths::get)
                .mapTry(Files::newBufferedReader)
                .getOrElseThrow(e -> new UncheckedIOException(new IOException("File " + file + " not found")));
    }

    public static InputStream urlAsStream(String url) {
        return Try.of(() -> new URL(url).openStream())
                .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
    }

    public static DocumentContext stringAsJson(String text) {
        return PARSER.parse(text);
    }

    public static Path get(String uri) throws URISyntaxException {
        return Paths.get(IOUtils.class.getResource(uri).toURI());
    }
}
