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
@ConfigurationProperties("podcastserver.externaltools")
public class ExternalTools {
    private String ffmpeg = "/usr/local/bin/ffmpeg";
    private String rtmpdump = "/usr/local/bin/rtmpdump";
}
