package lan.dk.podcastserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModules(
                        new Hibernate4Module()
                            .enable(Hibernate4Module.Feature.FORCE_LAZY_LOADING)
                            .disable(Hibernate4Module.Feature.USE_TRANSIENT_ANNOTATION),
                        new JavaTimeModule()
                )
        ;
    }

}
