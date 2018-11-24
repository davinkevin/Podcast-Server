package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Tag;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.util.UUID;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 02/06/2017 for Podcast Server
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DatabaseConfigurationTest.class})
public class TagRepositoryTest {

    private @Autowired DataSource dataSource;
    private @Autowired TagRepository tagRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    private static final Operation INSERT_TAG_DATA = sequenceOf(
            insertInto("TAG")
                    .columns("ID", "NAME")
                    .values(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
                    .values(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
                    .values(UUID.fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")
                    .build()
    );

    @Before
    public void prepare() throws Exception {
        Operation operation = sequenceOf(DELETE_ALL, INSERT_TAG_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void should_find_tag_by_id() {
        /* GIVEN */
        dbSetupTracker.skipNextLaunch();
        UUID id = UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08");
        /* WHEN  */
        Tag tag = tagRepository.findById(id).get();
        /* THEN  */
        assertThat(tag.getId()).isEqualTo(id);
        assertThat(tag.getName()).isEqualTo("Foo");
    }

    @Test
    public void should_find_by_name_ignoring_case() {
        /* GIVEN */
        String name = "bar";
        /* WHEN  */
        Option<Tag> aTag = tagRepository.findByNameIgnoreCase(name);
        /* THEN  */
        assertThat(aTag).isNotEmpty();
        assertThat(aTag.get().getName()).isEqualTo("bAr");
        assertThat(aTag.get().getId()).isEqualTo(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"));
    }

    @Test
    public void should_find_by_containing_ignoring_case() {
        /* GIVEN */
        String name = "bar";
        Tag t1 = new Tag();
        t1.setId(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"));
        t1.setName("bAr");

        Tag t2 = new Tag();
        t2.setId(UUID.fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"));
        t2.setName("Another Bar");

        /* WHEN  */
        Set<Tag> tags = tagRepository.findByNameContainsIgnoreCase(name);
        /* THEN  */
        assertThat(tags).isNotEmpty().contains(t1, t2);
    }


}