package lan.dk.podcastserver.config;

import com.github.davinkevin.podcastserver.service.TikaProbeContentType;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.service.properties.Api;
import lan.dk.podcastserver.service.properties.Backup;
import lan.dk.podcastserver.service.properties.ExternalTools;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.apache.tika.Tika;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kevin on 26/12/2013.
 */
@EnableCaching
@Configuration
@EnableConfigurationProperties({PodcastServerParameters.class, Api.class, Backup.class, ExternalTools.class})
@ComponentScan(basePackages = {
        "com.github.davinkevin.podcastserver.service", "com.github.davinkevin.podcastserver.business", "com.github.davinkevin.podcastserver.manager",
        "lan.dk.podcastserver.service", "lan.dk.podcastserver.manager"
})
public class BeanConfigScan {

    @Bean(name="Validator")
    LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean TikaProbeContentType tikaProbeContentType() {
        return new TikaProbeContentType(new Tika());
    }

    @Bean
    @ConfigurationPropertiesBinding
    Converter<String, Path> pathConverter() {
        return new Converter<String, Path>(){

            @Override
            public Path convert(String source) {
                return Paths.get(source);
            }
        };
    }

    @Bean
    Converter<String, Set<String>> stringToSet() {
        return new Converter<String, Set<String>>() {
            @Override
            public Set<String> convert(String s) {
                return HashSet.of(s.split(","));
            }
        };
    }
}
