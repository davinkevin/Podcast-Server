package lan.dk.podcastserver.service;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

/**
 * Created by kevin on 03/02/15.
 */
@Service
@Setter
@ConfigurationProperties(value = "podcastserver"/*, ignoreUnknownFields = false*/)
public class PodcastServerParameters {

    /**
     * Location of Podcasts
     */
    String rootfolder = "/Users/kevin/Tomcat/podcast/webapps/podcast/";
    /**
     * Url of the Server
     */
    String serverUrl = "http://localhost:8080";
    /**
     * URL to fileContainer
     */
    String fileContainer = "http://localhost:8080/podcast";
    /**
     * Filename of the cover
     */
    String coverDefaultName = "cover";
    /**
     * Temp file extension during download
     */
    String downloadExtension = ".psdownload";

    /**
     * Max Update podcast in parallel
     */
    Integer maxUpdateParallels = 3;
    /**
     * Number of concurrent download
     */
    Integer concurrentDownload = 3;
    /**
     * Number of retry to download an item
     */
    Integer numberOfTry = 10;
    /**
     * Number of day to download
     */
    Long numberOfDayToDownload = 30L;
    /**
     * Number of item by default in RSS exposed by the Podcast Server
     */
    Long rssDefaultNumberItem = 50L;

    /**
     * Api Key of different provider
     */
    Api api;

    /**
     * Path to external tools used by Podcast-Server
     * @return
     */
    ExternalTools externalTools;

    //** GETTER OF THE PARAMETERS **//
    public String getRootfolder() {
        return rootfolder;
    }
    public String getServerUrl() {
        return serverUrl;
    }
    public String getDownloadExtension() {
        return downloadExtension;
    }
    public Api getApi() {
        return api;
    }
    public Api api() {
        return api;
    }
    public Path rootFolder() { return Paths.get(rootfolder); }
    public String rootFolderWithProtocol() { return "file://".concat(rootfolder); }
    public URI fileContainer() throws URISyntaxException { return new URI(fileContainer); }
    public URI serverUrl() throws URISyntaxException { return new URI(serverUrl);}
    public Long numberOfDayToDownload() { return numberOfDayToDownload; }
    public Integer maxUpdateParallels() { return maxUpdateParallels; }
    public Integer concurrentDownload() { return concurrentDownload; }
    public Integer numberOfTry() {return numberOfTry;}
    public String coverDefaultName() { return coverDefaultName;}
    public Long rssDefaultNumberItem() {
        return rssDefaultNumberItem;
    }



    /* Utils on attributes */
    public ZonedDateTime limitDownloadDate() {
        return now().minusDays(numberOfDayToDownload);
    }


    @Getter @Setter
    @Accessors(chain = true)
    public static class Api {
        private String youtube;
        private String dailymotion;

        public String youtube() {
            return youtube;
        }
        public String dailymotion() {
            return dailymotion;
        }
    }

    @Getter @Setter
    @Accessors(chain = true)
    public static class ExternalTools {
        private String ffmpeg;
        private String rtmpdump;

        public String ffmpeg(){return ffmpeg;}
        public String rtmpdump(){return rtmpdump;}
    }
}
