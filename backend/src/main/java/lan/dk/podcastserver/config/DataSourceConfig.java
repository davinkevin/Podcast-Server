package lan.dk.podcastserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

import static java.time.ZonedDateTime.now;

/**
 * Created by kevin on 11/04/15
 */
@Configuration
@ComponentScan("lan.dk.podcastserver.repository")
@EnableTransactionManagement
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.username:sa}") String user,
            @Value("${spring.datasource.password:}") String password,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.driver-class-name}") String driveClassName) throws SQLException {

        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setDriverClassName(driveClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);

        return dataSource;
    }

    @Bean
    DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(now());
    }
}
