package lan.dk.podcastserver.context.Mock;

import lan.dk.podcastserver.service.api.ItemService;
import lan.dk.podcastserver.service.api.PodcastService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MockService {
    @Bean
    public static ItemService getItemService() {
        return Mockito.mock(ItemService.class);
    }

    @Bean
    public static PodcastService getPodcastService() {
        return Mockito.mock(PodcastService.class);
    }
}
