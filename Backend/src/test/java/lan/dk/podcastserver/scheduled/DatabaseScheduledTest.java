package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.service.DatabaseService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Created by kevin on 29/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseScheduledTest {

    @Mock DatabaseService databaseService;
    @InjectMocks DatabaseScheduled databaseScheduled;

    @Test
    public void should_launch_backup() throws IOException {
        /* When */
        databaseScheduled.backup();

        /* Then */
        verify(databaseService, only()).backupWithDefault();
    }

    @Test
    public void should_be_scheduled_for_4am_or_by_property() throws NoSuchMethodException {
        /* Given */
        /* When */
        String cronValue = ((Scheduled) DatabaseScheduled.class.getMethod("backup").getDeclaredAnnotations()[0]).cron();
        /* Then */
        assertThat(cronValue).contains("${podcastserver.backup.cron:", "0 0 4 * * *", "}");
    }

}