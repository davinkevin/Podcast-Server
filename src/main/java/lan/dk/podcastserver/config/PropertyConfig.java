package lan.dk.podcastserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Created by kevin on 26/12/2013.
 */
@Configuration
/*@PropertySource(value = {"classpath:application.properties"}, ignoreResourceNotFound = true)*/
@PropertySource(value = {"classpath:application.properties", "file:${catalina.home}/conf/podcastserver.properties"}, ignoreResourceNotFound = true)
public class PropertyConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
