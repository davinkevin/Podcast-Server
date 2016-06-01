package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.service.BackupService;
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
public class DatabaseBackupScheduledTest {

    @Mock BackupService backupService;
    @InjectMocks DatabaseBackupScheduled databaseBackupScheduled;

    @Test
    public void should_launch_backup() throws IOException {
        /* When */
        databaseBackupScheduled.backup();

        /* Then */
        verify(backupService, only()).backupWithDefault();
    }

    @Test
    public void should_be_schedulled_for_4am_or_by_property() throws NoSuchMethodException {
        /* Given */
        /* When */
        String cronValue = ((Scheduled) DatabaseBackupScheduled.class.getMethod("backup").getDeclaredAnnotations()[0]).cron();
        /* Then */
        assertThat(cronValue).contains("${podcastserver.backup.cron:", "0 0 4 * * *", "}");
    }

}