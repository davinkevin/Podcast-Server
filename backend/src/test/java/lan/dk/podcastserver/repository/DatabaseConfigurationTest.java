package lan.dk.podcastserver.repository;

import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.operation.Operation;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static java.time.ZonedDateTime.now;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@Configuration
@EnableJpaRepositories(basePackages = "lan.dk.podcastserver.repository")
@EntityScan(basePackages = "lan.dk.podcastserver.entity")
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class DatabaseConfigurationTest {

    private static final Operation DELETE_ALL_PODCASTS = deleteAllFrom("PODCAST");
    private static final Operation DELETE_ALL_ITEMS = deleteAllFrom("ITEM");
    private static final Operation DELETE_ALL_TAGS = sequenceOf(deleteAllFrom("PODCAST_TAGS"), deleteAllFrom("TAG"));
    private static final Operation DELETE_ALL_PLAYLIST = Operations.sequenceOf(deleteAllFrom("WATCH_LIST_ITEMS"), deleteAllFrom("WATCH_LIST"));

    public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(" ").append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter();
    public static final Operation DELETE_ALL = sequenceOf(DELETE_ALL_PLAYLIST, DELETE_ALL_ITEMS, DELETE_ALL_TAGS, DELETE_ALL_PODCASTS, DELETE_ALL_TAGS);

    @Bean DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(now());
    }


}
