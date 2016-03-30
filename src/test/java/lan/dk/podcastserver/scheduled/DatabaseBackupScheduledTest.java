package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.service.BackupService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Paths;

import static lan.dk.podcastserver.service.PodcastServerParameters.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 29/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseBackupScheduledTest {

    private static final Backup BACKUP_PARAMETERS = new Backup().setBinary(true).setCron("0 0 4 * * *").setLocation(Paths.get("/tmp").toFile());
    @Mock BackupService backupService;
    @Mock PodcastServerParameters parameters;
    @InjectMocks DatabaseBackupScheduled databaseBackupScheduled;

    @Test
    public void should_launch_backup() throws IOException {
        /* Given */
        when(parameters.backup()).thenReturn(BACKUP_PARAMETERS);

        /* When */
        databaseBackupScheduled.backup();

        /* Then */
        verify(parameters, atLeast(1)).backup();
        verify(backupService, only()).backup(eq(BACKUP_PARAMETERS.location()), eq(BACKUP_PARAMETERS.getBinary()));
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