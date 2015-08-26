package lan.dk.podcastserver.config;

import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
public class BeanConfigScanTest {

    @Test
    public void should_get_validator() {
        /* Given */
        BeanConfigScan beanConfigScan = new BeanConfigScan();
        /* When */
        LocalValidatorFactoryBean validator = beanConfigScan.validator();
        /* Then */
        assertThat(validator)
                .isNotNull()
                .isOfAnyClassIn(LocalValidatorFactoryBean.class);
    }
}