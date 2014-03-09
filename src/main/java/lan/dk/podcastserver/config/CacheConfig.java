package lan.dk.podcastserver.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Created by kevin on 08/03/2014.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {

        Cache cachePodcast = new ConcurrentMapCache("podcast");
        Cache cacheItem = new ConcurrentMapCache("item");

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(cachePodcast, cacheItem));

        return manager;
    }

}
