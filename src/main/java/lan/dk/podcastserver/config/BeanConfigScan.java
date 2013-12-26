package lan.dk.podcastserver.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by kevin on 26/12/2013.
 */
@Configuration
@ComponentScan(basePackages = { "lan.dk.podcastserver.manager",
                                "lan.dk.podcastserver.utils",
                                "lan.dk.podcastserver.business"})

public class BeanConfigScan {
}
