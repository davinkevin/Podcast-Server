package lan.dk.podcastserver.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Created by kevin on 04/02/2014.
 */
@Configuration
@PropertySource(value = {"classpath:application.properties"}, ignoreResourceNotFound = true)
public class PropertyConfigTest {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

}
