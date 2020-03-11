package lan.dk.podcastserver.service;

import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Service
public class JsonService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JsonService.class);
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

    public arrow.core.Option<DocumentContext> parseUrl(String url) {
        try(var page = urlService.asReader(url)) {
            var asJson = parse(page.lines().collect(joining()));
            return Option.Companion.fromNullable(asJson);
        } catch (Exception e) {
            log.error("Error during fetching of each items of {}", url, e);
            return Option.Companion.empty();
        }
    }

    public static <T> Function<DocumentContext, T> to(Class<T> clazz) {
        return JsonService.to("$", clazz);
    }

    public static <T> Function<DocumentContext, T> to(String from, Class<T> clazz) {
        return d -> d.read(from, clazz);
    }
}
