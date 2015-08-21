package lan.dk.podcastserver.config;

import lan.dk.podcastserver.service.PodcastServerParameters;
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
public class AsyncConfigTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @InjectMocks AsyncConfig asyncConfig;

    @Test
    public void should_generate_multi_thread_executor() {
        /* Given */
        when(podcastServerParameters.concurrentDownload()).thenReturn(10);

        /* When */
        TaskExecutor asyncExecutor = asyncConfig.getAsyncExecutor();

        /* Then */
        assertThat(asyncExecutor)
                .isNotNull()
                .isOfAnyClassIn(ThreadPoolTaskExecutor.class, AsyncTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getCorePoolSize())
                .isEqualTo(10);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getMaxPoolSize())
                .isEqualTo(10);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getThreadNamePrefix())
                .contains("Update");
    }

    @Test
    public void should_generate_single_thread_executor() {
        /* When */
        TaskExecutor asyncExecutor = asyncConfig.singleThreadExecutor();

        /* Then */
        assertThat(asyncExecutor)
                .isNotNull()
                .isOfAnyClassIn(ThreadPoolTaskExecutor.class, AsyncTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getCorePoolSize())
                .isEqualTo(1);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getMaxPoolSize())
                .isEqualTo(1);
        assertThat(((ThreadPoolTaskExecutor) asyncExecutor).getThreadNamePrefix())
                .contains("Manual");
    }
    
    @Test
    public void should_generate_exception_handler() {
        assertThat(asyncConfig.getAsyncUncaughtExceptionHandler()).isNull();
    }

}