package com.github.davinkevin.podcastserver.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.GzipResourceResolver

/**
 * Configuration de la partie Web MVC de l'application
 * Plus d'informations sur le configuration Java-Config de Spring MVC : http://www.luckyryan.com/2013/02/07/migrate-spring-mvc-servlet-xml-to-java-config/
 */
@Configuration
@ComponentScan("lan.dk.podcastserver.controller", "com.github.davinkevin.podcastserver.controller")
@EnableSpringDataWebSupport
class WebMvcConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val gzipResourceResolver = GzipResourceResolver()

        registry.apply {
            addResourceHandler("*.js", "*.css", "*.js.min", "*.css.min")
                    .addResourceLocations("classpath:/static/")
                    .setCachePeriod(CACHE_PERIOD)
                    .resourceChain(true)
                    .addResolver(gzipResourceResolver)

            addResourceHandler("fonts/*.woff", "fonts/*.woff2", "fonts/*.eot", "fonts/*.svg", "fonts/*.ttf")
                    .addResourceLocations("classpath:/static/fonts/")
                    .setCachePeriod(CACHE_PERIOD)
                    .resourceChain(true)
                    .addResolver(gzipResourceResolver)
        }
    }

    override fun configureDefaultServletHandling(configurer: DefaultServletHandlerConfigurer) {
        configurer.enable()
    }

    companion object {
        private const val CACHE_PERIOD = 31556926
    }
}
