package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.config.WebMvcConfig
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer
import org.springframework.web.servlet.config.annotation.ResourceChainRegistration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class WebMvcConfigTest {

    @InjectMocks lateinit var webMvcConfig: WebMvcConfig

    @Test
    fun should_add_resource_handlers() {
        /* Given */
        val registry = mock<ResourceHandlerRegistry>()
        val resourceHandlerRegistration = mock<ResourceHandlerRegistration>()
        val resourceChainRegistration = mock<ResourceChainRegistration>()

        whenever(registry.addResourceHandler(anyVararg())).thenReturn(resourceHandlerRegistration)
        whenever(resourceHandlerRegistration.addResourceLocations(any())).thenReturn(resourceHandlerRegistration)
        whenever(resourceHandlerRegistration.setCachePeriod(any())).thenReturn(resourceHandlerRegistration)
        whenever(resourceHandlerRegistration.resourceChain(any())).thenReturn(resourceChainRegistration)
        whenever(resourceChainRegistration.addResolver(any())).thenReturn(resourceChainRegistration)

        /* When */
        webMvcConfig.addResourceHandlers(registry)

        /* Then */
        verify(registry, times(2)).addResourceHandler(anyVararg())
    }

    @Test
    fun should_configure_servlet_handling() {
        /* Given */
        val configurer = mock<DefaultServletHandlerConfigurer>()

        /* When */
        webMvcConfig.configureDefaultServletHandling(configurer)

        /* Then */
        verify(configurer, times(1)).enable()
    }
}
