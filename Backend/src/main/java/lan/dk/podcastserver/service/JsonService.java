package lan.dk.podcastserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import javaslang.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Slf4j
@Service
public class JsonService {

    private final UrlService urlService;
    private final ParseContext parserContext;

    @Autowired
    public JsonService(UrlService urlService, ObjectMapper mapper) {
        this.urlService = urlService;
        this.parserContext = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider(mapper)).build());
    }

    public DocumentContext parse(String json) {
        return parserContext.parse(json);
    }

    public Option<DocumentContext> parseUrl(String url) {
        try (BufferedReader bufferedReader = urlService.asReader(url)) {
            return Option.of(parse(bufferedReader.lines().collect(joining())));
        } catch (IOException e) {
            log.error("Error during fetching of each items of {}", url, e);
            return Option.none();
        }
    }

    public static <T> Function<DocumentContext, T> to(Class<T> clazz) {
        return JsonService.to("$", clazz);
    }

    public static <T> Function<DocumentContext, T> to(String from, Class<T> clazz) {
        return d -> d.read(from, clazz);
    }

    public static <T> Function<DocumentContext, T> to(TypeRef<T> typeRef) {
        return JsonService.to("$", typeRef);
    }

    public static <T> Function<DocumentContext, T> to(String from, TypeRef<T> typeRef) {
        return d -> d.read(from, typeRef);
    }

    public static <T> Function<DocumentContext, T> extract(String path) {
        return d -> d.read(path);
    };
}
