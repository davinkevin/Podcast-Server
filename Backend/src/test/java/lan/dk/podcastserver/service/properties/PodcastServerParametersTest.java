package lan.dk.podcastserver.service.properties;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.time.ZonedDateTime.now;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/07/15 for Podcast Server
 */
public class PodcastServerParametersTest {

    private PodcastServerParameters parameters;

    @Before
    public void beforeEach() { parameters = new PodcastServerParameters(); }

    @Test
    public void should_have_default_value() throws URISyntaxException {
        Path ROOT_FOLDER = Paths.get("/tmp/");

        assertThat(parameters)
                .hasRootfolder(ROOT_FOLDER)
                .hasDownloadExtension(".psdownload")
                .hasNumberOfDayToDownload(30L)
                .hasNumberOfDayToSaveCover(365L)
        ;

        assertThat(parameters.getRootfolder()).isEqualTo(ROOT_FOLDER);
        assertThat(parameters.getCoverDefaultName()).isEqualTo("cover");
        assertThat(parameters.getConcurrentDownload()).isEqualTo(3);
        assertThat(parameters.getNumberOfTry()).isEqualTo(10);
        assertThat(parameters.getRssDefaultNumberItem()).isEqualTo(50L);
        assertThat(parameters.limitToKeepCoverOnDisk())
                .isAfter(now().minusYears(1).minusDays(10))
                .isBefore(now().minusDays(364));
    }

    @Test
    public void should_have_modified_values() throws URISyntaxException {
        /* Given */
        Path ROOT_FOLDER = Paths.get("/tmp");

        /* When */
        parameters.setRootfolder(ROOT_FOLDER);
        parameters.setCoverDefaultName("default");
        parameters.setDownloadExtension(".psdownload");
        parameters.setMaxUpdateParallels(5);
        parameters.setConcurrentDownload(5);
        parameters.setNumberOfTry(20);
        parameters.setNumberOfDayToDownload(5L);
        parameters.setNumberOfDayToSaveCover(5L);
        parameters.setRssDefaultNumberItem(25L);

        /* Then */
        assertThat(parameters)
                .hasRootfolder(ROOT_FOLDER)
                .hasDownloadExtension(".psdownload");

        assertThat(parameters.getRootfolder()).isEqualTo(ROOT_FOLDER);
        assertThat(parameters.getCoverDefaultName()).isEqualTo("default");

        assertThat(parameters.getConcurrentDownload()).isEqualTo(5);
        assertThat(parameters.getNumberOfTry()).isEqualTo(20);

        assertThat(parameters.getRssDefaultNumberItem()).isEqualTo(25L);
        assertThat(parameters.limitDownloadDate())
                .isBeforeOrEqualTo(now().minusDays(parameters.getNumberOfDayToDownload()))
                .isAfterOrEqualTo(now().minusDays(parameters.getNumberOfDayToDownload()).minusMinutes(1));
    }

}
