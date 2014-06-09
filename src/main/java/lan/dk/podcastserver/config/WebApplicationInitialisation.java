package lan.dk.podcastserver.config;

import org.h2.server.web.WebServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

/**
 * Class permettant d'initier la configuration de globale de l'application.
 * Cette classe se substitue au Web.xml.
 */

@Order(1)
public class WebApplicationInitialisation extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Value("${numberofdaytodownload:3}")
    private int numberofdaytodownload;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{WebMvcConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    /**
     * @param servletContext
     * @throws javax.servlet.ServletException
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {


        // Définition des profiles actifs :
        //System.setProperty("spring.profiles.active", "data-mysql");


        ServletRegistration.Dynamic h2Servlet = servletContext.addServlet("h2console", WebServlet.class);
        h2Servlet.setLoadOnStartup(2);
        h2Servlet.addMapping("/console/database/*");


        super.onStartup(servletContext);
    }
}