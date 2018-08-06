package lan.dk.podcastserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceConfigTest.DataSourceConfigTestMocks.class, DataSourceConfig.class, HibernateJpaAutoConfiguration.class})
public class DataSourceConfigTest {

    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String DATA_SOURCE_URL = "jdbc:h2:mem:memoryDatabase";
    private static final String DATA_SOURCE_CLASS_NAME = "org.h2.Driver";

    private @Autowired DataSource dataSource;
    private @Autowired DateTimeProvider dateTimeProvider;

    @Test
    public void should_be_configuration() {
        assertThat(DataSourceConfig.class).hasAnnotation(Configuration.class);
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

    @Test
    public void should_have_a_now_date_time_provider() {
        /* Given */
        /* When */
        Optional<TemporalAccessor> now = dateTimeProvider.getNow();
        /* Then */
        assertThat(Instant.from(now.get())).isBeforeOrEqualTo(Instant.now());
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
            properties.setProperty("spring.jpa.properties.hibernate.search.default.indexmanager", "near-real-time");
            properties.setProperty("spring.jpa.properties.hibernate.search.default.directory_provider", "ram");
            properties.setProperty("spring.jpa.properties.hibernate.search.default.indexBase", "/tmp/lucene");

            pspc.setProperties(properties);
            return pspc;
        }
    }
}
