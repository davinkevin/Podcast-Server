package lan.dk.podcastserver.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kevin on 03/02/15.
 */
@Service
@ConfigurationProperties("podcastserver")
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
    String downloadExtention = ".psdownload";

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

    //** GETTER OF THE PARAMETERS **//
    public String getRootfolder() {
        return rootfolder;
    }
    public String getServerUrl() {
        return serverUrl;
    }

    public String getDownloadExtention() {
        return downloadExtention;
    }

    public Path rootFolder() { return Paths.get(rootfolder); }
    public String rootFolderWithProtocol() { return "file://".concat(rootfolder); }
    public URI serveurURL() throws URISyntaxException { return new URI(serverUrl); }
    public URI fileContainer() throws URISyntaxException { return new URI(fileContainer); }
    public Long numberOfDayToDownload() { return numberOfDayToDownload; }
    public Integer maxUpdateParallels() { return maxUpdateParallels; }
    public Integer concurrentDownload() { return concurrentDownload; }
    public Integer numberOfTry() {return numberOfTry;}
    public String coverDefaultName() { return coverDefaultName;}
    public Long rssDefaultNumberItem() {
        return rssDefaultNumberItem;
    }

    public void setRootfolder(String rootfolder) {
        this.rootfolder = rootfolder;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setFileContainer(String fileContainer) {
        this.fileContainer = fileContainer;
    }

    public void setCoverDefaultName(String coverDefaultName) {
        this.coverDefaultName = coverDefaultName;
    }

    public void setDownloadExtention(String downloadExtention) {
        this.downloadExtention = downloadExtention;
    }

    public void setMaxUpdateParallels(Integer maxUpdateParallels) {
        this.maxUpdateParallels = maxUpdateParallels;
    }

    public void setConcurrentDownload(Integer concurrentDownload) {
        this.concurrentDownload = concurrentDownload;
    }

    public void setNumberOfTry(Integer numberOfTry) {
        this.numberOfTry = numberOfTry;
    }

    public void setNumberOfDayToDownload(Long numberOfDayToDownload) {
        this.numberOfDayToDownload = numberOfDayToDownload;
    }

    public void setRssDefaultNumberItem(Long rssDefaultNumberItem) {
        this.rssDefaultNumberItem = rssDefaultNumberItem;
    }
}
