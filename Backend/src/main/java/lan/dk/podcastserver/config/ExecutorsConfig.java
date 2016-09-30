package lan.dk.podcastserver.config;

import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;

/**
 * Created by kevin on 08/02/2014.
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = { "lan.dk.podcastserver.manager"})
public class ExecutorsConfig {

    @Resource PodcastServerParameters podcastServerParameters;

    @Bean(name = "UpdateExecutor")
    public ThreadPoolTaskExecutor updateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(podcastServerParameters.getConcurrentDownload());
        executor.setMaxPoolSize(podcastServerParameters.getConcurrentDownload());
        executor.setThreadNamePrefix("Update-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "ManualUpdater")
    public ThreadPoolTaskExecutor singleThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("Manual-Update-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "DownloadExecutor")
    public ThreadPoolTaskExecutor downloadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(podcastServerParameters.getConcurrentDownload());
        executor.setThreadNamePrefix("Downloader-");
        executor.initialize();
        return executor;
    }
}
