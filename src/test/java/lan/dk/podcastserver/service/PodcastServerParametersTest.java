package lan.dk.podcastserver.service;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/07/15 for Podcast Server
 */
public class PodcastServerParametersTest {

    @Test
    public void should_have_default_value() throws URISyntaxException {
        String ROOT_FOLDER = "/tmp/";

        PodcastServerParameters parameters = new PodcastServerParameters();
        parameters.setApi(new PodcastServerParameters.Api().setDailymotion("DAILYMOTION_API_KEY").setYoutube("YOUTUBE_API_KEY"));

        PodcastServerParametersAssert.assertThat(parameters)
                .hasRootfolder(ROOT_FOLDER)
                .hasServerUrl("http://localhost:8080")
                .hasDownloadExtension(".psdownload");

        assertThat(parameters.rootFolder())
                .isEqualTo(Paths.get(ROOT_FOLDER));

        assertThat(parameters.rootFolderWithProtocol())
                .isEqualTo("file://" + ROOT_FOLDER);

        assertThat(parameters.fileContainer())
                .isEqualTo(new URI(parameters.fileContainer));

        assertThat(parameters.coverDefaultName())
                .isEqualTo("cover");

        assertThat(parameters.maxUpdateParallels())
                .isEqualTo(3);

        assertThat(parameters.concurrentDownload())
                .isEqualTo(3);

        assertThat(parameters.numberOfTry())
                .isEqualTo(10);

        assertThat(parameters.numberOfDayToDownload())
                .isEqualTo(30L);

        assertThat(parameters.rssDefaultNumberItem())
                .isEqualTo(50L);

        assertThat(parameters.serverUrl())
                .isEqualTo(new URI("http://localhost:8080"));

        assertThat(parameters.getApi()).isNotNull();
        assertThat(parameters.api().dailymotion()).isEqualTo("DAILYMOTION_API_KEY");
        assertThat(parameters.api().youtube()).isEqualTo("YOUTUBE_API_KEY");
    }
    
    @Test
    public void should_have_mdificated_values() throws URISyntaxException {
        /* Given */
        String ROOT_FOLDER = "/tmp";
        PodcastServerParameters parameters = new PodcastServerParameters();

        /* When */
        parameters.setRootfolder(ROOT_FOLDER);
        parameters.setServerUrl("http://localhost:9191");
        parameters.setFileContainer("http://localhost:9191/podcast");
        parameters.setCoverDefaultName("default");
        parameters.setDownloadExtension(".pdownload");
        parameters.setMaxUpdateParallels(5);
        parameters.setConcurrentDownload(5);
        parameters.setNumberOfTry(20);
        parameters.setNumberOfDayToDownload(5L);
        parameters.setRssDefaultNumberItem(25L);

        /* Then */
        PodcastServerParametersAssert.assertThat(parameters)
                .hasRootfolder(ROOT_FOLDER)
                .hasServerUrl("http://localhost:9191")
                .hasDownloadExtension(".pdownload");

        assertThat(parameters.rootFolder()).isEqualTo(Paths.get(ROOT_FOLDER));
        assertThat(parameters.rootFolderWithProtocol()).isEqualTo("file://" + ROOT_FOLDER);
        assertThat(parameters.fileContainer()).isEqualTo(new URI("http://localhost:9191/podcast"));
        assertThat(parameters.coverDefaultName()).isEqualTo("default");
        assertThat(parameters.maxUpdateParallels()).isEqualTo(5);
        assertThat(parameters.concurrentDownload()).isEqualTo(5);
        assertThat(parameters.numberOfTry()).isEqualTo(20);
        assertThat(parameters.numberOfDayToDownload()).isEqualTo(5L);
        assertThat(parameters.rssDefaultNumberItem()).isEqualTo(25L);
        assertThat(parameters.limitDownloadDate())
                .isBeforeOrEqualTo(now().minusDays(parameters.numberOfDayToDownload()))
                .isAfterOrEqualTo(now().minusDays(parameters.numberOfDayToDownload()).minusMinutes(1));
    }

    @Test
    public void should_have_api() {
        /* Given */
        /* When */
        PodcastServerParameters.Api api = new PodcastServerParameters.Api()
                .setYoutube("Foo")
                .setDailymotion("Bar");

        /* Then */
        assertThat(api.youtube()).isEqualTo("Foo");
        assertThat(api.dailymotion()).isEqualTo("Bar");
        assertThat(api.getYoutube()).isEqualTo("Foo");
        assertThat(api.getDailymotion()).isEqualTo("Bar");
    }

    @Test
    public void should_have_external_tools() {
        /* Given */
        /* When */
        PodcastServerParameters.ExternalTools externalTools = new PodcastServerParameters.ExternalTools()
                .setFfmpeg("Foo")
                .setRtmpdump("Bar");

        PodcastServerParameters podcastServerParameters = new PodcastServerParameters().setExternalTools(externalTools);

        /* Then */
        assertThat(externalTools.ffmpeg()).isEqualTo("Foo");
        assertThat(externalTools.rtmpdump()).isEqualTo("Bar");
        assertThat(externalTools.getFfmpeg()).isEqualTo("Foo");
        assertThat(externalTools.getRtmpdump()).isEqualTo("Bar");
        assertThat(podcastServerParameters.rtmpDump()).isEqualTo("Bar");
    }

}
