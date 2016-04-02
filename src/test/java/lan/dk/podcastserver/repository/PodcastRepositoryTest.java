package lan.dk.podcastserver.repository;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DatabaseConfiguraitonTest.class, HibernateJpaAutoConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public class PodcastRepositoryTest {

    @Autowired DataSource dataSource;
    @Autowired PodcastRepository podcastRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    public static final Operation INSERT_REFERENCE_DATA = sequenceOf(
                    insertInto("PODCAST")
                            .columns("ID", "TITLE", "URL")
                            .values(UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "AppLoad", null)
                            .values(UUID.fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD", "http://fake.url.com/rss")
                            .build()
                    );


    @Before
    public void prepare() throws Exception {
        Operation operation = sequenceOf(DatabaseConfiguraitonTest.DELETE_ALL, INSERT_REFERENCE_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void should_find_by_url_not_null() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        List<Podcast> podcasts = podcastRepository.findByUrlIsNotNull();

        /* Then */
        assertThat(podcasts)
                .hasSize(1)
                .extracting(Podcast::getTitle)
                .contains("Geek Inc HD");
    }
}