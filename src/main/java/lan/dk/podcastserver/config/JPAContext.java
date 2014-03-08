package lan.dk.podcastserver.config;

import com.jolbox.bonecp.BoneCPDataSource;
import org.h2.tools.Server;
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
import java.sql.SQLException;
import java.util.Properties;

/**
 * Définition de la configuration associée à la partie persistence
 *
 */

@Configuration
@ComponentScan(basePackages = {"lan.dk.podcastserver.repository", "lan.dk.podcastserver.entity"})
@EnableJpaRepositories("lan.dk.podcastserver.repository")
@Profile("!data-embedded")
public class JPAContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String PROPERTY_NAME_DATABASE_DRIVER = "db.driver";
    private static final String PROPERTY_NAME_DATABASE_PASSWORD = "db.password";
    private static final String PROPERTY_NAME_DATABASE_URL = "db.url";
    private static final String PROPERTY_NAME_DATABASE_USERNAME = "db.username";

    private static final String PROPERTY_NAME_HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String PROPERTY_NAME_HIBERNATE_FORMAT_SQL = "hibernate.format_sql";
    private static final String PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY = "hibernate.ejb.naming_strategy";
    private static final String PROPERTY_NAME_HIBERNATE_SHOW_SQL = "hibernate.show_sql";
    private static final String PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN = "entitymanager.packages.to.scan";
    private static final String PROPERTY_NAME_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

    private static final String PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER = "hibernate.search.default.directory_provider";
    private static final String PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_INDEXBASE = "hibernate.search.default.indexBase";

    public static final String[] PARAMETER_H2_SERVER = new String[]{"-tcp", "-tcpAllowOthers", "-tcpPort", "9999"};

    @Resource
    private Environment environment;

    /**
     * Définition de la Datasource
     *
     * @return
     */
    @Bean
    public DataSource dataSource() throws SQLException {
        BoneCPDataSource dataSource = new BoneCPDataSource();

        dataSource.setDriverClass(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_DRIVER));
        dataSource.setJdbcUrl(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_URL));

        if (environment.containsProperty(PROPERTY_NAME_DATABASE_USERNAME))
            dataSource.setUsername(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_USERNAME));
        if (environment.containsProperty(PROPERTY_NAME_DATABASE_PASSWORD))
            dataSource.setPassword(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_PASSWORD));

        if (environment.getRequiredProperty(PROPERTY_NAME_DATABASE_URL).contains(":h2:tcp://"))
            h2Server();

        return dataSource;
    }

    /**
     * Création du transaction Manager
     *
     * @return
     * @throws ClassNotFoundException
     */
    @Bean
    public JpaTransactionManager transactionManager() throws ClassNotFoundException, SQLException {
        JpaTransactionManager transactionManager = new JpaTransactionManager();

        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());

        return transactionManager;
    }

    /**
     * Création de l'entityManager spécifique au contexxte / profil courant.
     *
     * @return
     * @throws ClassNotFoundException
     */

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws ClassNotFoundException, SQLException {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPackagesToScan(environment.getRequiredProperty(PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN));
        entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        Properties jpaProperties = new Properties();
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_DIALECT, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_DIALECT));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_FORMAT_SQL, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_FORMAT_SQL));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_SHOW_SQL, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SHOW_SQL));

        //jpaProperties.put(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER));
        //jpaProperties.put(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_INDEXBASE, environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SEARCH_DEFAULT_INDEXBASE));

        jpaProperties.put(PROPERTY_NAME_HBM2DDL_AUTO, "update");

        entityManagerFactoryBean.setJpaProperties(jpaProperties);

        return entityManagerFactoryBean;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2Server() throws SQLException {
        return Server.createTcpServer(PARAMETER_H2_SERVER);
    }

}
