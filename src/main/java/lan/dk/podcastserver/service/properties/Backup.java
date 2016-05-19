package lan.dk.podcastserver.service.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@Getter @Setter
@Accessors(chain = true)
@ConfigurationProperties("podcastserver.backup")
public class Backup {
    private Path location;
    private Boolean binary = false;
    private String cron = "0 0 4 * * *";
}
