package lan.dk.podcastserver.context.Mock;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.PodcastBusiness;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MockService {
    @Bean
    public static ItemBusiness getItemService() {
        return Mockito.mock(ItemBusiness.class);
    }

    @Bean
    public static PodcastBusiness getPodcastService() {
        return Mockito.mock(PodcastBusiness.class);
    }
}
