package lan.dk.podcastserver.service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import javaslang.control.Option;
import javaslang.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Created by kevin on 21/07/2016.
 */
@Slf4j
@Service
public class UrlService {

    public static final String USER_AGENT_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
    public static final String USER_AGENT_MOBILE = "AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)";
    static final String USER_AGENT_KEY = "User-agent";

    private static final String PROTOCOL_SEPARATOR = "://";
    private static final Integer MAX_NUMBER_OF_REDIRECTION = 10;
    private static final Consumer<HttpURLConnection> NO_MODIFICATION = x -> {};

    public UrlService() {System.setProperty("http.agent", USER_AGENT_DESKTOP);}

    /* Get, Post and Other standard request of UniREST */
    public GetRequest get(String url) { return Unirest.get(url); }
    public HttpRequestWithBody post(String url) {
        return Unirest.post(url);
    }

    /* Real Url business */
    public String getRealURL(String url) {
        return getRealURL(url, NO_MODIFICATION, 0);
    }
    public String getRealURL(String url, Consumer<HttpURLConnection> connectionModifier) {
        return getRealURL(url, connectionModifier, 0);
    }
    private String getRealURL(String url, Consumer<HttpURLConnection> connectionModifier, Integer numberOfRedirection) {
        if (MAX_NUMBER_OF_REDIRECTION <= numberOfRedirection) {
            throw new RuntimeException("Too many redirects");
        }

        HttpURLConnection con = Try.of(() -> new URL(url))
                .mapTry(URL::openConnection)
                .map(HttpURLConnection.class::cast)
                .andThen(connectionModifier)
                .andThen(c -> c.setInstanceFollowRedirects(false))
                .onFailure(e -> log.error("Error during retrieval of the real URL for {} at {} redirection", url, numberOfRedirection, e))
                .get();

        Option<Integer> isRedirect = Try.of(con::getResponseCode)
                .andThenTry(con::disconnect)
                .filter(this::isARedirection)
                .getOption();

        String location = isRedirect.isDefined() ? addDomainIfRelative(url, con.getHeaderField("Location")) : "";

        return isRedirect
                .map(r -> getRealURL(location, connectionModifier, numberOfRedirection+1))
                .getOrElse(url);

    }
    private Boolean isARedirection(int status) {
        return status != HttpURLConnection.HTTP_OK && (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER
        );
    }

    /* Transform to Stream or Reader */
    public InputStream asStream(String url) throws IOException {
        return Try.of(() -> get(url))
                .mapTry(BaseRequest::asBinary)
                .map(HttpResponse::getBody)
                .getOrElseThrow(e -> new IOException(e));
    }
    public BufferedReader asReader(String url) throws IOException {
        return new BufferedReader(new InputStreamReader(asStream(url)));
    }

    /* Relative and absolute URL transformation */
    public String addDomainIfRelative(String urlWithDomain, String mayBeRelativeUrl) {
        if (mayBeRelativeUrl.contains(PROTOCOL_SEPARATOR))
            return mayBeRelativeUrl;

        Boolean isFromRoot = mayBeRelativeUrl.startsWith("/");

        return Try.of(() -> new URL(urlWithDomain))
                .map(u -> u.getProtocol() + PROTOCOL_SEPARATOR + u.getAuthority() + (isFromRoot ? "" : StringUtils.substringBeforeLast(u.getPath(), "/")))
                .map(s -> s + (isFromRoot ? "" : "/") + mayBeRelativeUrl)
                .getOrElseThrow(e -> new RuntimeException(e));

        /*return StringUtils.substringBeforeLast(urlWithDomain, "/") + "/" + mayBeRelativeUrl;*/
    }

}
