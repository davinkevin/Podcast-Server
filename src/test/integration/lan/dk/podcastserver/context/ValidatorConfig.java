package lan.dk.podcastserver.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Created by kevin on 13/07/2014.
 */
@Configuration
@ComponentScan(basePackages = { "lan.dk.podcastserver.manager.worker.updater"} )
public class ValidatorConfig {

    @Bean(name="Validator")
    public Validator getValidator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
