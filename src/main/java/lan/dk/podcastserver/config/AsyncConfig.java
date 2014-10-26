package lan.dk.podcastserver.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Created by kevin on 08/02/2014.
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = { "lan.dk.podcastserver.manager"})
public class AsyncConfig implements AsyncConfigurer {

    @Value("${maxUpdateParallels:3}") private int concurrentDownload;

    @Bean(name = "UpdateExecutor")
    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrentDownload);
        executor.setMaxPoolSize(concurrentDownload);
        executor.setThreadNamePrefix("Update-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}
