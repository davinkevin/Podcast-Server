package lan.dk.podcastserver.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by kevin on 26/12/2013.
 */
@Configuration
@ComponentScan(basePackages = { "lan.dk.podcastserver.utils",
                                "lan.dk.podcastserver.business"})
@EnableAsync
public class BeanConfigScan {
}
