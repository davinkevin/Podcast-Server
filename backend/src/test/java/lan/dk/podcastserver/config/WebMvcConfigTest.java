package lan.dk.podcastserver.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceChainRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class WebMvcConfigTest {

    @InjectMocks WebMvcConfig webMvcConfig;

    @Test
    public void should_add_resource_handlers() {
        /* Given */
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration resourceHandlerRegistration = mock(ResourceHandlerRegistration.class);
        ResourceChainRegistration resourceChainRegistration = mock(ResourceChainRegistration.class);

        when(registry.addResourceHandler(anyVararg())).thenReturn(resourceHandlerRegistration);
        when(resourceHandlerRegistration.addResourceLocations(anyString())).thenReturn(resourceHandlerRegistration);
        when(resourceHandlerRegistration.setCachePeriod(anyInt())).thenReturn(resourceHandlerRegistration);
        when(resourceHandlerRegistration.resourceChain(anyBoolean())).thenReturn(resourceChainRegistration);
        when(resourceChainRegistration.addResolver(any())).thenReturn(resourceChainRegistration);

        /* When */
        webMvcConfig.addResourceHandlers(registry);

        /* Then */
        verify(registry, times(2)).addResourceHandler(anyVararg());
    }

    @Test
    public void should_configure_servlet_handling() {
        /* Given */
        DefaultServletHandlerConfigurer configurer = mock(DefaultServletHandlerConfigurer.class);

        /* When */
        webMvcConfig.configureDefaultServletHandling(configurer);

        /* Then */
        verify(configurer, times(1)).enable();
    }
}
