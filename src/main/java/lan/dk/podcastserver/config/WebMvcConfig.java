package lan.dk.podcastserver.config;

import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.utils.jackson.CustomObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.GzipResourceResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

/**
 * Configuration de la partie Web MVC de l'application
 * Plus d'informations sur le configuration Java-Config de Spring MVC : http://www.luckyryan.com/2013/02/07/migrate-spring-mvc-servlet-xml-to-java-config/
 */
@Configuration
@ComponentScan("lan.dk.podcastserver.controller")
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Resource PodcastServerParameters podcastServerParameters;
    @Value("${management.context-path:}") String actuatorPath = "";

    public static final int CACHE_PERIOD = 31556926;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        GzipResourceResolver gzipResourceResolver = new GzipResourceResolver();

        registry
                .addResourceHandler(Stream.of("js", "css").flatMap(ext -> Stream.of("/*." + ext, "/*." + ext + ".map")).toArray(String[]::new))
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(CACHE_PERIOD)
                .resourceChain(true)
                .addResolver(gzipResourceResolver);

        registry
                .addResourceHandler("/fonts/*")
                .addResourceLocations("classpath:/static/fonts/")
                .setCachePeriod(CACHE_PERIOD)
                .resourceChain(true)
                .addResolver(gzipResourceResolver);
    }

    @Bean
    public InternalResourceViewResolver getInternalResourceViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/");
        resolver.setSuffix(".html");
        return resolver;
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void configureMessageConverters (List<HttpMessageConverter<?>> converters) {

        //Ajout de la librairie de désierialization spécifiques à Hibernate pour Jackson
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new CustomObjectMapper());

        converters.add(mappingJackson2HttpMessageConverter);

        //Ajout du mapping string par défaut :
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        converters.add(stringHttpMessageConverter);
    }
}