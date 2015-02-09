package lan.dk.podcastserver.context;



import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"lan.dk.podcastserver.manager.worker"})
//@ComponentScan(basePackages = {"lan.dk.podcastserver.manager", "lan.dk.podcastserver.utils"})
public class MockWorkerContextConfiguration {

    @Bean
    public static ItemDownloadManager getItemDownloadManager() {
        ItemDownloadManager itemDownloadManager = Mockito.mock(ItemDownloadManager.class);
        /*when(itemDownloadManager.getServerURL()).thenReturn("http://192.168.1.210:8080");*/
        /*when(itemDownloadManager.getRootfolder()).thenReturn("/Users/kevin/");*/
        return itemDownloadManager;
    }


}

