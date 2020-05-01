package com.github.davinkevin.podcastserver.update.updaters.upload

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Created by kevin on 01/05/2020
 */
@Configuration
class UploadUpdaterConfig {

    @Bean
    fun uploadUpdater(): UploadUpdater = UploadUpdater()

}
