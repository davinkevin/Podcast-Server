package lan.dk.podcastserver.service;

import lan.dk.podcastserver.utils.PodcastServerParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PodcastServerParametersTest.PropertyConfig.class}, loader=AnnotationConfigContextLoader.class)
public class PodcastServerParametersTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Resource PodcastServerParameters podcastServerParameters;
    @Resource Environment environment;
    
    @Test
    public void should_have_a_root_folder() throws URISyntaxException {
        podcastServerParameters.rootFolder();
        assertThat(podcastServerParameters.getRootfolder()).isEqualTo("/Users/kevin/Tomcat/Tomcat 8/webapps/podcast");
    }
    
    @Test
    public void should_have_application_url () throws URISyntaxException {
        logger.info(podcastServerParameters.serveurURL().toString());
    }
    
    @Test
    public void should_load_value_from_env () {
        logger.info(environment.getProperty("rootfolder", "${catalina.home}/webapp/podcast/"));
    }
    

    @Configuration
    @ComponentScan("lan.dk.podcastserver.service")
    @PropertySource(value = {"classpath:properties/podcastServerParameterService.properties"})
    public static class PropertyConfig {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

    }
}
