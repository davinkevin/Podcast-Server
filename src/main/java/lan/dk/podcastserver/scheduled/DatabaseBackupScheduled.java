package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.service.BackupService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by kevin on 28/03/2016 for Podcast Server
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DatabaseBackupScheduled {

    final BackupService backupService;
    final PodcastServerParameters parameters;

    @Scheduled(cron="${podcastserver.backup.cron:0 0 4 * * *}")
    public void backup() throws IOException {
        backupService.backup(parameters.backup().location(), parameters.backup().getBinary());
    }

}
