package lan.dk.podcastserver.service.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@Getter @Setter
@Accessors(chain = true)
@ConfigurationProperties("podcastserver.api")
public class Api {
    private String youtube;
    private String dailymotion;
}
