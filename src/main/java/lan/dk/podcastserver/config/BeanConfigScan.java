package lan.dk.podcastserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Created by kevin on 26/12/2013.
 */
@Configuration
@ComponentScan(basePackages = { "lan.dk.podcastserver.utils",
                                "lan.dk.podcastserver.business"})
@EnableAsync
public class BeanConfigScan {

    @Bean(name="Validator")
    public LocalValidatorFactoryBean getValidator() {
        return new LocalValidatorFactoryBean();
    }
}
