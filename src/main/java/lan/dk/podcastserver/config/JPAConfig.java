package lan.dk.podcastserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.Server;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Définition de la configuration associée à la partie persistence
 *
 */

@Configuration
@ComponentScan(basePackages = {"lan.dk.podcastserver.repository", "lan.dk.podcastserver.entity", "lan.dk.podcastserver.business"})
@EnableJpaRepositories("lan.dk.podcastserver.repository")
@EnableTransactionManagement
public class JPAConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // DATASOURCE VALUES :
    private static final String PROPERTY_DATABASEDRIVER_NAME = "db.driver";
    private static final String PROPERTY_DATABASEDRIVER_DEFAULT = "org.h2.Driver";

    private static final String PROPERTY_DATABASEURL_NAME = "db.url";
    private static final String PROPERTY_DATABASEURL_DEFAULT = "jdbc:h2:mem:podcastserver;MVCC=TRUE";

    private static final String PROPERTY_DATABASEUSERNAME_NAME = "db.username";
    private static final String PROPERTY_DATABASEUSERNAME_DEFAULT = "sa";

    private static final String PROPERTY_DATABASEPASSWORD_NAME = "db.password";
    private static final String PROPERTY_DATABASEPASSWORD_DEFAULT = "";

    // HIBERNATE VALUES :
    private static final String PROPERTY_HIBERNATEDIALECT_NAME = "hibernate.dialect";
    private static final String PROPERTY_HIBERNATEDIALECT_DEFAULT = "org.hibernate.dialect.H2Dialect";

    private static final String PROPERTY_HIBERNATEFORMATSQL_NAME = "hibernate.format_sql";
    private static final String PROPERTY_HIBERNATEFORMATSQL_DEFAULT = "false";

    private static final String PROPERTY_HIBERNATENAMINGSTRATEGY_NAME = "hibernate.ejb.naming_strategy";
    private static final String PROPERTY_HIBERNATENAMINGSTRATEGY_DEFAULT = "org.hibernate.cfg.ImprovedNamingStrategy";

    private static final String PROPERTY_HIBERNATESHOWSQL_NAME = "hibernate.show_sql";
    private static final String PROPERTY_HIBERNATESHOWSQL_DEFAULT = "false";

    private static final String PROPERTY_ENTITYMANAGERPACKAGESTOSCAN_NAME = "entitymanager.packages.to.scan";
    private static final String PROPERTY_ENTITYMANAGER_PACKAGES_TO_SCAN_DEFAULT = "lan.dk.podcastserver.entity";

    private static final String PROPERTY_HBM2DDLAUTO_NAME = "hibernate.hbm2ddl.auto";
    private static final String PROPERTY_HBM2DDLAUTO_DEFAULT = "update";

    // Hibernate Search :
    private static final String PROPERTY_INDEXMANAGER_NAME = "hibernate.search.default.indexmanager";
    private static final String PROPERTY_INDEXMANAGER_DEFAULT = "near-real-time";

    private static final String PROPERTY_DIRECTORYPROVIDER_NAME = "hibernate.search.default.directory_provider";
    private static final String PROPERTY_DIRECTORYPROVIDER_DEFAULT = "ram";

    private static final String PROPERTY_INDEXBASE_NAME = "hibernate.search.default.indexBase";
    private static final String PROPERTY_INDEXBASE_DEFAULT = "/tmp/lucene";

    // H2 Database :
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

        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setDriverClassName(environment.getProperty(PROPERTY_DATABASEDRIVER_NAME, PROPERTY_DATABASEDRIVER_DEFAULT));
        dataSource.setJdbcUrl(environment.getProperty(PROPERTY_DATABASEURL_NAME, PROPERTY_DATABASEURL_DEFAULT));
        dataSource.setUsername(environment.getProperty(PROPERTY_DATABASEUSERNAME_NAME, PROPERTY_DATABASEUSERNAME_DEFAULT));
        dataSource.setPassword(environment.getProperty(PROPERTY_DATABASEPASSWORD_NAME, PROPERTY_DATABASEPASSWORD_DEFAULT));

        if (environment.getProperty(PROPERTY_DATABASEURL_NAME, PROPERTY_DATABASEURL_DEFAULT).contains(":h2:tcp://"))
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
    public PlatformTransactionManager transactionManager() throws ClassNotFoundException, SQLException {
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
        entityManagerFactoryBean.setPackagesToScan(environment.getProperty(PROPERTY_ENTITYMANAGERPACKAGESTOSCAN_NAME, PROPERTY_ENTITYMANAGER_PACKAGES_TO_SCAN_DEFAULT));
        entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        Properties jpaProperties = new Properties();
        jpaProperties.put(PROPERTY_HIBERNATEDIALECT_NAME, environment.getProperty(PROPERTY_HIBERNATEDIALECT_NAME, PROPERTY_HIBERNATEDIALECT_DEFAULT));
        jpaProperties.put(PROPERTY_HIBERNATEFORMATSQL_NAME, environment.getProperty(PROPERTY_HIBERNATEFORMATSQL_NAME, PROPERTY_HIBERNATEFORMATSQL_DEFAULT));
        jpaProperties.put(PROPERTY_HIBERNATENAMINGSTRATEGY_NAME, environment.getProperty(PROPERTY_HIBERNATENAMINGSTRATEGY_NAME, PROPERTY_HIBERNATENAMINGSTRATEGY_DEFAULT));
        jpaProperties.put(PROPERTY_HIBERNATESHOWSQL_NAME, environment.getProperty(PROPERTY_HIBERNATESHOWSQL_NAME, PROPERTY_HIBERNATESHOWSQL_DEFAULT));
        jpaProperties.put(PROPERTY_HBM2DDLAUTO_NAME, environment.getProperty(PROPERTY_HBM2DDLAUTO_NAME, PROPERTY_HBM2DDLAUTO_DEFAULT));

        // Hibernate Search :
        jpaProperties.put(PROPERTY_INDEXMANAGER_NAME, environment.getProperty(PROPERTY_INDEXMANAGER_NAME, PROPERTY_INDEXMANAGER_DEFAULT));
        jpaProperties.put(PROPERTY_DIRECTORYPROVIDER_NAME, environment.getProperty(PROPERTY_DIRECTORYPROVIDER_NAME, PROPERTY_DIRECTORYPROVIDER_DEFAULT));

        if ("filesystem".equals(environment.getProperty(PROPERTY_DIRECTORYPROVIDER_NAME, PROPERTY_DIRECTORYPROVIDER_DEFAULT))) {
            jpaProperties.put(PROPERTY_INDEXBASE_NAME, environment.getProperty(PROPERTY_INDEXBASE_NAME, PROPERTY_INDEXBASE_DEFAULT));
        }


        entityManagerFactoryBean.setJpaProperties(jpaProperties);

        return entityManagerFactoryBean;
    }

    @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }


    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2Server() throws SQLException {
        return Server.createTcpServer(PARAMETER_H2_SERVER);
    }

}
