package lan.dk.podcastserver.config;

import lan.dk.podcastserver.utils.jackson.HibernateAwareObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Configuration de la partie Web MVC de l'application
 * Plus d'informations sur le configuration Java-Config de Spring MVC : http://www.luckyryan.com/2013/02/07/migrate-spring-mvc-servlet-xml-to-java-config/
 */
@EnableWebMvc
@ComponentScan("lan.dk.podcastserver.controller")
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    public static final int CACHE_PERIOD = 31556926;
    private List<HttpMessageConverter<?>> messageConverters; // Cached: this is not a bean.

    /**
     * Enregistrement standards des Controllers
     *
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        super.addViewControllers(registry);
    }

    /**
     * Déclaration des ressources statics, par défaut la racine de la webapp est utilisé.
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //super.addResourceHandlers(registry);
        /*
        registry.addResourceHandler("/css/**").addResourceLocations("/app/css/").setCachePeriod(CACHE_PERIOD);
        registry.addResourceHandler("/img/**").addResourceLocations("/app/img/").setCachePeriod(CACHE_PERIOD);
        registry.addResourceHandler("/js/**").addResourceLocations("/app/js/").setCachePeriod(CACHE_PERIOD);
        registry.addResourceHandler("/font/**").addResourceLocations("/app/font/").setCachePeriod(CACHE_PERIOD);
        registry.addResourceHandler("/html/**").addResourceLocations("/app/html/").setCachePeriod(CACHE_PERIOD);
        registry.addResourceHandler("/less/**").addResourceLocations("/app/less/").setCachePeriod(CACHE_PERIOD);
        */
    }

    /**
     * Configuration du InternalResourceViewResolver associé à la localisation des JSPs.
     *
     * @return
     */
    @Bean
    public InternalResourceViewResolver getInternalResourceViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/pages/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    /**
     * Défini le <mvc:default-servlet-handler/>.
     *
     * @param configurer
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void configureMessageConverters (List<HttpMessageConverter<?>> converters) {

        super.configureMessageConverters(converters);

        //Ajout de la librairie de désierialization spécifiques à Hibernate pour Jackson
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new HibernateAwareObjectMapper());
        converters.add(mappingJackson2HttpMessageConverter);

        //Ajout du mapping string par défaut :
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        converters.add(stringHttpMessageConverter);
    }

}