package lan.dk.podcastserver.controller.task;

import com.github.davinkevin.podcastserver.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Created by kevin on 18/05/2016 for Podcast Server
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/task")
@ConditionalOnProperty("podcastserver.backup.enabled")
public class DatabaseController {

    private final DatabaseService databaseService;

    @GetMapping("backup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void backup() throws IOException {
        databaseService.backupWithDefault();
    }
}
