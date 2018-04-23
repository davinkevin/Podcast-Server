package lan.dk.podcastserver.service.properties;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
public class BackupTest {

    private Backup backup;

    @Before
    public void beforeEach() {
        backup = new Backup();
    }

    @Test
    public void should_have_default_value() {
        /* Given */
        /* When */
        /* Then */
        assertThat(backup)
                .hasLocation(null)
                .hasCron("0 0 4 * * *")
                .hasBinary(false);
    }

    @Test
    public void should_have_specified_values() {
        /* Given */
        /* When */
        backup
            .setBinary(true)
            .setCron("cron")
            .setLocation(Paths.get("/tmp"));

        /* Then */
        assertThat(backup)
                .hasBinary(true)
                .hasCron("cron")
                .hasLocation(Paths.get("/tmp"))
        ;
    }

}
