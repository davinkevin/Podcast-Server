package lan.dk.podcastserver.config;

import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ExecutorsConfigTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @InjectMocks ExecutorsConfig executorsConfig;

    @Test
    public void should_generate_multi_thread_executor() {
        /* Given */
        when(podcastServerParameters.getMaxUpdateParallels()).thenReturn(10);

        /* When */
        TaskExecutor asyncExecutor = executorsConfig.updateExecutor();

        /* Then */
        assertThat(asyncExecutor).isNotNull().isOfAnyClassIn(ThreadPoolTaskExecutor.class, AsyncTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getCorePoolSize()).isEqualTo(10);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getMaxPoolSize()).isEqualTo(10);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getThreadNamePrefix()).contains("Update");
    }

    @Test
    public void should_generate_single_thread_executor() {
        /* When */
        TaskExecutor asyncExecutor = executorsConfig.singleThreadExecutor();

        /* Then */
        assertThat(asyncExecutor).isNotNull().isOfAnyClassIn(ThreadPoolTaskExecutor.class, AsyncTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getCorePoolSize()).isEqualTo(1);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getMaxPoolSize()).isEqualTo(1);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getThreadNamePrefix()).contains("Manual");
    }

    @Test
    public void should_generate_download_thread_executor() {
        /* Given */
        when(podcastServerParameters.getConcurrentDownload()).thenReturn(10);

        /* When */
        ThreadPoolTaskExecutor executor = executorsConfig.downloadExecutor();

        /* Then */
        assertThat(executor).isNotNull().isOfAnyClassIn(ThreadPoolTaskExecutor.class, AsyncTaskExecutor.class);
        assertThat(executor.getCorePoolSize()).isEqualTo(10);
        assertThat(executor.getThreadNamePrefix()).contains("Downloader");
    }

}
