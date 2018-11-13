package lan.dk.podcastserver.service.properties;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

/**
 * Created by kevin on 03/02/15.
 */
@Setter
@Accessors(chain = true)
@ConfigurationProperties(value = "podcastserver"/*, ignoreUnknownFields = false*/)
public class PodcastServerParameters {

    /**
     * Location of Podcasts
     */
    Path rootfolder = Paths.get("/tmp");
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
    Integer maxUpdateParallels = 256;
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
     * Number of day to keep cover on disk
     */
    Long numberOfDayToSaveCover = 365L;

    /**
     * Number of item by default in RSS exposed by the Podcast Server
     */
    Long rssDefaultNumberItem = 50L;

    public Long getRssDefaultNumberItem() {
        return rssDefaultNumberItem;
    }


    //** GETTER OF THE PARAMETERS **//
    public ZonedDateTime limitDownloadDate() { return now().minusDays(numberOfDayToDownload); }
    public ZonedDateTime limitToKeepCoverOnDisk() { return now().minusDays(numberOfDayToSaveCover); }

    public Path getRootfolder() {
        return this.rootfolder;
    }

    public String getCoverDefaultName() {
        return this.coverDefaultName;
    }

    public String getDownloadExtension() {
        return this.downloadExtension;
    }

    public Integer getMaxUpdateParallels() {
        return this.maxUpdateParallels;
    }

    public Integer getConcurrentDownload() {
        return this.concurrentDownload;
    }

    public Integer getNumberOfTry() {
        return this.numberOfTry;
    }

    public Long getNumberOfDayToDownload() {
        return this.numberOfDayToDownload;
    }

    public Long getNumberOfDayToSaveCover() {
        return this.numberOfDayToSaveCover;
    }
}
