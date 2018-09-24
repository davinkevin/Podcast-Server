package lan.dk.podcastserver.service.properties;

import lombok.Getter;
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
@Setter @Getter
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
}
