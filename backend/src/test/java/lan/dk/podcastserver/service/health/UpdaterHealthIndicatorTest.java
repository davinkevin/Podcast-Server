package lan.dk.podcastserver.service.health;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 20/07/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdaterHealthIndicatorTest {

    @Mock UpdatePodcastBusiness updater;
    @InjectMocks UpdaterHealthIndicator updaterHealthIndicator;

    @Test
    public void should_generate_health_information() {
        /* Given */
        ZonedDateTime now = ZonedDateTime.now();
        when(updater.getLastFullUpdate()).thenReturn(now);
        when(updater.isUpdating()).thenReturn(Boolean.TRUE);
        when(updater.getUpdaterActiveCount()).thenReturn(5);

        /* When */
        Health health = updaterHealthIndicator.health();

        /* Then */
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).contains(
                entry("lastFullUpdate", now),
                entry("isUpdating", Boolean.TRUE),
                entry("activeThread", 5)
        );
    }

}
