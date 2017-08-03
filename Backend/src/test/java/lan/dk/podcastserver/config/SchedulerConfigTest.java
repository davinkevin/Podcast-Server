package lan.dk.podcastserver.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SchedulerConfigTest {

    @InjectMocks SchedulerConfig schedulerConfig;

    @Test
    public void should_have_config_injected() {
        assertThat(schedulerConfig).isNotNull();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void should_have_spring_annotations() {
        assertThat(schedulerConfig.getClass())
                .hasAnnotations(Configuration.class, EnableScheduling.class);
    }
}
