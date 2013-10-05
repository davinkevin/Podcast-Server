package lan.dk.podcastserver.context;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@Configuration
@PropertySource("classpath:application.properties")
//@ComponentScan(basePackages = {"lan.dk.podcastserver.manager.worker"})
@ComponentScan(basePackages = {"lan.dk.podcastserver.manager", "lan.dk.podcastserver.utils"})
public class MockWorkerContextConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer GlobalPropertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer p = new PropertySourcesPlaceholderConfigurer();
        org.springframework.core.io.Resource[] resourceLocations = new org.springframework.core.io.Resource[] {
                new ClassPathResource("application.properties")
        };
        p.setLocations(resourceLocations);
        return p;
    }

//    @Bean
//    public static ItemDownloadManager getItemDownloadManager() {
//        ItemDownloadManager itemDownloadManager = Mockito.mock(ItemDownloadManager.class);
//        when(itemDownloadManager.getServerURL()).thenReturn("http://192.168.1.210:8080");
//        when(itemDownloadManager.getRootfolder()).thenReturn("/Users/kdavin/Downloads/Logiciels Projet/Apache Tomcat/Tomcat 7/apache-tomcat-7.0.28/webapps/podcast");
//        return itemDownloadManager;
//    }


}

