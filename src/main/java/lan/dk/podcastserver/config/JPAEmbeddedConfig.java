package lan.dk.podcastserver.config;

import com.jolbox.bonecp.BoneCPDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {"lan.dk.podcastserver.repository", "lan.dk.podcastserver.entity"})
@EnableJpaRepositories("lan.dk.podcastserver.repository")
@Profile("data-embedded")
public class JPAEmbeddedConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String PROPERTY_NAME_HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String PROPERTY_NAME_HIBERNATE_FORMAT_SQL = "hibernate.format_sql";
    private static final String PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY = "hibernate.ejb.naming_strategy";
    private static final String PROPERTY_NAME_HIBERNATE_SHOW_SQL = "hibernate.show_sql";
    private static final String PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN = "entitymanager.packages.to.scan";
    private static final String PROPERTY_NAME_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

    private static final String PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER = "hibernate.search.default.directory_provider";
    private static final String PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_INDEXBASE = "hibernate.search.default.indexBase";

    @Resource
    private Environment environment;

    @Bean
    public DataSource dataSource() {
        /*
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            EmbeddedDatabase ed = builder.setType(EmbeddedDatabaseType.H2).addScript("test-script.sql").build();
        */

        BoneCPDataSource dataSource = new BoneCPDataSource();

        logger.info("Instanciation de la database embarquée");

        dataSource.setDriverClass("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:podcastserver;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");


        return dataSource;
    }


    /**
     * Création du transaction Manager
     *
     * @return
     * @throws ClassNotFoundException
     */
    @Bean
    public JpaTransactionManager transactionManager() throws ClassNotFoundException {
        JpaTransactionManager transactionManager = new JpaTransactionManager();

        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());

        return transactionManager;
    }


    /**
     * Création de l'entityManager spécifique au contexxte / profil courant.
     * Place le l'indexation en RAM
     *
     * @return
     * @throws ClassNotFoundException
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws ClassNotFoundException {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPackagesToScan(environment.getRequiredProperty(PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN));
        entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        Properties jpaProperties = new Properties();
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_DIALECT, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_DIALECT));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_FORMAT_SQL, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_FORMAT_SQL));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_SHOW_SQL, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SHOW_SQL));

        // Initialisation de la database
        jpaProperties.put(PROPERTY_NAME_HBM2DDL_AUTO, "create-drop");

        // Gestion du cache :
        //jpaProperties.setProperty("hibernate.cache.use_second_level_cache", "true");
        //jpaProperties.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        //jpaProperties.setProperty("hibernate.cache.use_query_cache", "true");
        //jpaProperties.setProperty("hibernate.generate_statistics", "true");

        //jpaProperties.put(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER, "ram");
        //jpaProperties.put(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_INDEXBASE, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_INDEXBASE));

        entityManagerFactoryBean.setJpaProperties(jpaProperties);

        return entityManagerFactoryBean;
    }

}
