package lan.dk.podcastserver.context;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by kevin on 22/02/15.
 */
@Configuration
@ComponentScan(basePackages = { "lan.dk.podcastserver.service"} )
public class ServiceConfig {
}
