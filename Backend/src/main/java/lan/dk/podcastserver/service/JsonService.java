package lan.dk.podcastserver.service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class JsonService {

    private final UrlService urlService;
    private final ParseContext parserContext = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build());

    public DocumentContext parse(String json) {
        return parserContext.parse(json);
    }

    public Optional<DocumentContext> parse(URL url) {
        try (BufferedReader bufferedReader = urlService.urlAsReader(url)) {
            return Optional.of(parse(bufferedReader.lines().collect(joining())));
        } catch (IOException e) {
            log.error("Error during fetching of each items of {}", url, e);
            return Optional.empty();
        }
    }
}
