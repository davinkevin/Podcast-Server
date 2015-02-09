package lan.dk.podcastserver.config;

import lan.dk.podcastserver.utils.PodcastServerParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * Created by kevin on 09/02/15.
 */
@Configuration
public class PodcastServerConfig {

    @Resource Environment environment;

    private static final String PROPERTY_ROOTFOLDER_NAME = "rootfolder";
    private static final String PROPERTY_ROOTFOLDER_DEFAULT = "#environment.getProperty('catalina.home').concat('/webapps/podcast/')";

    private static final String PROPERTY_SERVEURURL_NAME = "serverURL";
    private static final String PROPERTY_SERVEURURL_DEFAULT = "http://localhost:8080";

    private static final String PROPERTY_FILECONTAINER_NAME = "fileContainer";
    private static final String PROPERTY_FILECONTAINER_DEFAULT = "http://localhost:8080/podcast";

    private static final String PROPERTY_COVERDEFAULTNAME_NAME = "cover.defaultname";
    private static final String PROPERTY_COVERDEFAULTNAME_DEFAULT = "cover";

    private static final String PROPERTY_DOWNLOADEXTENTION_NAME = "download.extention";
    private static final String PROPERTY_DOWNLOADEXTENTION_DEFAULT = ".psdownload";
    
    private static final String PROPERTY_NUMBEROFDAYTODOWNLOAD_NAME = "numberofdaytodownload";
    private static final Long PROPERTY_NUMBEROFDAYTODOWNLOAD_DEFAULT = 30L;

    private static final String PROPERTY_MAXUPDATEPARALLELS_NAME = "maxUpdateParallels";
    private static final Integer PROPERTY_MAXUPDATEPARALLELS_DEFAULT = 3;

    private static final String PROPERTY_CONCURRENTDOWNLOAD_NAME = "concurrentDownload";
    private static final Integer PROPERTY_CONCURRENTDOWNLOAD_DEFAULT = 3;

    private static final String PROPERTY_NUMBEROFTRY_NAME = "numberOfTry";
    private static final Integer PROPERTY_NUMBEROFTRY_DEFAULT = 10;

    @Bean
    PodcastServerParameters podcastServerParameters() {
        return PodcastServerParameters.builder(environment)
                    .rootfolder(environment.getProperty(PROPERTY_ROOTFOLDER_NAME, PROPERTY_ROOTFOLDER_DEFAULT))
                    .serveurURL(environment.getProperty(PROPERTY_SERVEURURL_NAME, PROPERTY_SERVEURURL_DEFAULT))
                    .fileContainer(environment.getProperty(PROPERTY_FILECONTAINER_NAME, PROPERTY_FILECONTAINER_DEFAULT))
                    .downloadExtention(environment.getProperty(PROPERTY_DOWNLOADEXTENTION_NAME, PROPERTY_DOWNLOADEXTENTION_DEFAULT))
                    .coverDefaultName(environment.getProperty(PROPERTY_COVERDEFAULTNAME_NAME, PROPERTY_COVERDEFAULTNAME_DEFAULT))
                    .numberOfDayToDownload(environment.getProperty(PROPERTY_NUMBEROFDAYTODOWNLOAD_NAME, Long.class, PROPERTY_NUMBEROFDAYTODOWNLOAD_DEFAULT))
                    .maxUpdateParallels(environment.getProperty(PROPERTY_MAXUPDATEPARALLELS_NAME, Integer.class, PROPERTY_MAXUPDATEPARALLELS_DEFAULT))
                    .concurrentDownload(environment.getProperty(PROPERTY_CONCURRENTDOWNLOAD_NAME, Integer.class, PROPERTY_CONCURRENTDOWNLOAD_DEFAULT))
                    .numberOfTry(environment.getProperty(PROPERTY_NUMBEROFTRY_NAME, Integer.class, PROPERTY_NUMBEROFTRY_DEFAULT))
                .build();
    } 
}
