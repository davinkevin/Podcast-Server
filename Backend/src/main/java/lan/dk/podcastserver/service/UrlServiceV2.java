package lan.dk.podcastserver.service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by kevin on 21/07/2016.
 */
@Slf4j
@Service
public class UrlServiceV2 {

    public GetRequest get(String url) {
        return Unirest.get(url);
    }

    public HttpRequestWithBody post(String url) {
        return Unirest.post(url);
    }
}
