package lan.dk.podcastserver.context.Mock;

import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MockRepository {
    @Bean
    public static PodcastRepository getPodcastRepository() {
        return Mockito.mock(PodcastRepository.class);
    }
    @Bean
    public static ItemRepository getItemRepository() {
        return Mockito.mock(ItemRepository.class);
    }
    @Bean
    public static CoverRepository getCoverRepository() {
        return Mockito.mock(CoverRepository.class);
    }

}
