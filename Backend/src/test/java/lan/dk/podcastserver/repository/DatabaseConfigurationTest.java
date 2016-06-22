package lan.dk.podcastserver.repository;

import com.ninja_squad.dbsetup.operation.Operation;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.GregorianCalendar;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static java.time.ZonedDateTime.now;
import static org.hibernate.search.jpa.Search.getFullTextEntityManager;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@Configuration
@EnableJpaRepositories(basePackages = "lan.dk.podcastserver.repository")
@EntityScan(basePackages = "lan.dk.podcastserver.entity")
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class DatabaseConfigurationTest {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }

    @Bean
    @Autowired
    public FullTextEntityManager fullTextEntityManager(EntityManager entityManager) {
        return getFullTextEntityManager(entityManager);
    }

    @Bean
    DateTimeProvider dateTimeProvider() {
        return () -> GregorianCalendar.from(now());
    }

    private static final Operation DELETE_ALL_PODCASTS = deleteAllFrom("PODCAST");
    private static final Operation DELETE_ALL_ITEMS = deleteAllFrom("ITEM");
    private static final Operation DELETE_ALL_TAGS = sequenceOf(deleteAllFrom("PODCAST_TAGS"), deleteAllFrom("TAG"));
    public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(" ").append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter();
    public static final Operation DELETE_ALL = sequenceOf(WatchListRepositoryTest.DELETE_ALL_PLAYLIST, DELETE_ALL_ITEMS, DELETE_ALL_TAGS, DELETE_ALL_PODCASTS, DELETE_ALL_TAGS);
}
