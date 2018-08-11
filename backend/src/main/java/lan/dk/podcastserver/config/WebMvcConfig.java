package lan.dk.podcastserver.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.GzipResourceResolver;

/**
 * Configuration de la partie Web MVC de l'application
 * Plus d'informations sur le configuration Java-Config de Spring MVC : http://www.luckyryan.com/2013/02/07/migrate-spring-mvc-servlet-xml-to-java-config/
 */
@Configuration
@ComponentScan("lan.dk.podcastserver.controller")
@EnableSpringDataWebSupport
public class WebMvcConfig implements WebMvcConfigurer {

    private static final int CACHE_PERIOD = 31556926;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        GzipResourceResolver gzipResourceResolver = new GzipResourceResolver();

        registry
                .addResourceHandler("*.js", "*.css", "*.js.min", "*.css.min")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(CACHE_PERIOD)
                .resourceChain(true)
                .addResolver(gzipResourceResolver);

        registry
                .addResourceHandler("fonts/*.woff", "fonts/*.woff2", "fonts/*.eot", "fonts/*.svg", "fonts/*.ttf")
                .addResourceLocations("classpath:/static/fonts/")
                .setCachePeriod(CACHE_PERIOD)
                .resourceChain(true)
                .addResolver(gzipResourceResolver);
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
