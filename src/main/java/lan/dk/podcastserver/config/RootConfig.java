package lan.dk.podcastserver.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Récupération des éléments présents dans le package lan.dk.podcastserver.config
 */
@Configuration
@ComponentScan(value = "lan.dk.podcastserver.config",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = RootConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebMvcConfig.class)
        })
public class RootConfig {

}
