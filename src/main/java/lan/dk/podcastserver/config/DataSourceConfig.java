package lan.dk.podcastserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by kevin on 11/04/15
 */
@Configuration
class DataSourceConfig {

    @Value("${spring.datasource.username:}")
    private String user;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String dataSourceClassName;

    // H2 Database :
    public static final String[] PARAMETER_H2_SERVER = new String[]{"-tcp", "-tcpAllowOthers", "-tcpPort", "9999"};

    @Bean
    public DataSource primaryDataSource() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setDriverClassName(dataSourceClassName);
        dataSource.setJdbcUrl(dataSourceUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);

        if (dataSourceUrl.contains(":h2:tcp://"))
            h2Server();

        return dataSource;
    }

    @Lazy
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2Server() throws SQLException { return Server.createTcpServer(PARAMETER_H2_SERVER); }
}