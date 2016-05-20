package lan.dk.podcastserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceConfigTest.DataSourceConfigTestMocks.class, DataSourceConfig.class, HibernateJpaAutoConfiguration.class})
public class DataSourceConfigTest {

    public static final String USER = "sa";
    public static final String PASSWORD = "";
    public static final String DATA_SOURCE_URL = "jdbc:h2:tcp://localhost:9999/~/Library/H2Database/testdb";
    public static final String DATA_SOURCE_CLASS_NAME = "org.h2.Driver";

    @Resource DataSource dataSource;
    @Resource Server h2Server;

    @Test
    public void should_be_configuration() {
        assertThat(DataSourceConfig.class)
                .hasAnnotation(Configuration.class);
    }

    @Test
    public void should_generate_datasource() throws SQLException {
        assertThat(dataSource).isOfAnyClassIn(HikariDataSource.class);
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        assertThat(hikariDataSource.getJdbcUrl()).isEqualTo(DATA_SOURCE_URL);
        assertThat(hikariDataSource.getUsername()).isEqualTo(USER);
        assertThat(hikariDataSource.getPassword()).isEqualTo(PASSWORD);
        assertThat(hikariDataSource.getDriverClassName()).isEqualTo(DATA_SOURCE_CLASS_NAME);
    }

    static class DataSourceConfigTestMocks {

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
            final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
            final Properties properties = new Properties();

            properties.setProperty("spring.datasource.username", USER);
            properties.setProperty("spring.datasource.password", PASSWORD);
            properties.setProperty("spring.datasource.url", DATA_SOURCE_URL);
            properties.setProperty("spring.datasource.driver-class-name", DATA_SOURCE_CLASS_NAME);
            pspc.setProperties(properties);
            return pspc;
        }
}
}