package lan.dk.podcastserver.scheduled;

import com.github.davinkevin.podcastserver.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by kevin on 28/03/2016 for Podcast Server
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("podcastserver.backup.enabled")
public class DatabaseScheduled {

    private final DatabaseService databaseService;

    @Scheduled(cron="${podcastserver.backup.cron:0 0 4 * * *}")
    public void backup() throws IOException {
        databaseService.backupWithDefault();
    }
}
