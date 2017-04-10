package lan.dk.podcastserver.service.health;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

/**
 * Created by kevin on 18/07/2016.
 */
@Component
@RequiredArgsConstructor
public class UpdaterHealthIndicator extends AbstractHealthIndicator {

    private final UpdatePodcastBusiness updater;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // @formatter:off
        builder.up()
            .withDetail("lastFullUpdate", isNull(updater.getLastFullUpdate()) ? "none" : updater.getLastFullUpdate())
            .withDetail("isUpdating", updater.isUpdating())
            .withDetail("activeThread", updater.getUpdaterActiveCount())
        .build();
        // @formatter:on
    }
}
