package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.item.ItemConfig
import com.github.davinkevin.podcastserver.podcast.PodcastConfig
import com.github.davinkevin.podcastserver.podcast.type.TypeConfig
import com.github.davinkevin.podcastserver.service.TikaProbeContentType
import com.github.davinkevin.podcastserver.service.properties.Api
import com.github.davinkevin.podcastserver.service.properties.Backup
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.tag.TagConfig
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.collection.Set
import org.apache.tika.Tika
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.convert.converter.Converter
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by kevin on 26/12/2013.
 */
@EnableCaching
@Configuration
@EnableConfigurationProperties(PodcastServerParameters::class, Api::class, Backup::class, ExternalTools::class)
@Import(ItemConfig::class, PodcastConfig::class, TypeConfig::class, TagConfig::class)
@ComponentScan(basePackages = [
    "com.github.davinkevin.podcastserver.service", "com.github.davinkevin.podcastserver.business", "com.github.davinkevin.podcastserver.manager", "com.github.davinkevin.podcastserver.config",
    "lan.dk.podcastserver.service"])
class BeanConfigScan {

    @Bean(name = ["Validator"])
    fun validator() = LocalValidatorFactoryBean()

    @Bean
    fun tikaProbeContentType() = TikaProbeContentType(Tika())

    @Bean
    @ConfigurationPropertiesBinding
    fun pathConverter() = Converter<String, Path> { source -> Paths.get(source) }

    @Bean
    fun stringToSet() = Converter<String, Set<String>> { it.split(",").toSet().toVΛVΓ() }
}
