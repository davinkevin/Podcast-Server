package lan.dk.podcastserver.service;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Slf4j
@Service
public class JsonService {

    final UrlService urlService;
    final JSONParser parser;

    @Autowired
    public JsonService(UrlService urlService) {
        this.urlService = urlService;
        this.parser = new JSONParser();
    }

    public Optional<JSONObject> asJson(String url) {
        try (BufferedReader bufferedReader = urlService.urlAsReader(url)) {
            return Optional.of((JSONObject) parser.parse(bufferedReader));
        } catch (IOException | ParseException e) {
            log.error("Error during fetching of each items of {}", url, e);
            return Optional.empty();
        }
    }

    public Optional<JSONObject> parse(String text) {
        try {
            return Optional.of((JSONObject) parser.parse(text));
        } catch (ParseException e) {
            log.error("Error during fetching of {}", text, e);
            return Optional.empty();
        }
    }
}
