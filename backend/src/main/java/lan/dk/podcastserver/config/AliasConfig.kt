package lan.dk.podcastserver.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Created by kevin on 2018-11-24
 */
@Configuration
@ComponentScan(basePackages = [
    "com.github.davinkevin.podcastserver.service", "com.github.davinkevin.podcastserver.business", "com.github.davinkevin.podcastserver.manager", "com.github.davinkevin.podcastserver.config",
    "lan.dk.podcastserver.service"])
class AliasConfig