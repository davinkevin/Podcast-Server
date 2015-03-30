package lan.dk.podcastserver.config;

import lan.dk.podcastserver.service.PodcastServerParameters;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;

/**
 * Created by kevin on 08/02/2014.
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = { "lan.dk.podcastserver.manager"})
public class AsyncConfig implements AsyncConfigurer {

    /*@Value("${maxUpdateParallels:3}") private int concurrentDownload;*/
    @Resource PodcastServerParameters podcastServerParameters;
    

    @Bean(name = "UpdateExecutor")
    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(podcastServerParameters.concurrentDownload());
        executor.setMaxPoolSize(podcastServerParameters.concurrentDownload());
        executor.setThreadNamePrefix("Update-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "ManualUpdater")
    public AsyncTaskExecutor singleThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("Manual-Update-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}
