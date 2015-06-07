package lan.dk.podcastserver.context;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by kevin on 19/07/2014.
 */
@Configuration
@ComponentScan(basePackages = { "lan.dk.podcastserver.utils"})
public class UtilsConfig {
}
