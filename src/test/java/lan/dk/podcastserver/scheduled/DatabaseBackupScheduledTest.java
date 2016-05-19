package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.service.BackupService;
import lan.dk.podcastserver.service.properties.Backup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 29/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseBackupScheduledTest {

    @Mock Backup backup;
    @Mock BackupService backupService;
    @InjectMocks DatabaseBackupScheduled databaseBackupScheduled;

    @Test
    public void should_launch_backup() throws IOException {
        /* Given */
        when(backup.getBinary()).thenReturn(true);
        when(backup.getCron()).thenReturn("0 0 4 * * *");
        when(backup.getLocation()).thenReturn(Paths.get("/tmp"));

        /* When */
        databaseBackupScheduled.backup();

        /* Then */
        verify(backup, times(1)).getBinary();
        verify(backup, times(1)).getLocation();
        verify(backupService, only()).backup(eq(Paths.get("/tmp")), eq(true));
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