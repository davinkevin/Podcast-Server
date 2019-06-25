package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.Status;
import com.github.davinkevin.podcastserver.manager.worker.Type;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import io.vavr.API;
import io.vavr.collection.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static java.time.ZonedDateTime.now;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_ITEM_DATA;
import static lan.dk.podcastserver.repository.dsl.ItemDSL.hasStatus;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 04/09/15 for Podcast Server
 */
@DataJpaTest
@Import(DatabaseConfigurationTest.class)
public class ItemRepositoryTest {

    @Autowired DataSource dataSource;
    @Autowired ItemRepository itemRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    @BeforeEach
    public void prepare() {
        Operation operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void should_find_all_to_download() {
        dbSetupTracker.skipNextLaunch();
        /* Given */
        ZonedDateTime date = now().minusDays(15);

        /* When */
        Set<Item> itemToDownload = itemRepository.findAllToDownload(date, 5);

        itemToDownload
                .map(Item::getTitle)
                .forEach(API::println);

        /* Then */
        assertThat(itemToDownload).hasSize(2);
        assertThat(itemToDownload)
                .extracting(Item::getTitle)
                .contains("Appload 3", "Geek INC 122");
    }

    @Test
    public void should_find_by_status() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Set<Item> itemByStatus = itemRepository.findByStatus(Status.FINISH, Status.DELETED);

        /* Then */
        assertThat(itemByStatus)
                .hasSize(3)
                .extracting("title")
                .containsOnly("Geek INC 124", "Geek INC 123", "Appload 1");
    }

    @Test
    public void should_find_by_type_and_expression() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Set<Item> byTypeAndExpression = itemRepository.findByTypeAndExpression(new Type("YOUTUBE", "YOUTUBE"), hasStatus(Status.FINISH));

        /* Then */
        assertThat(byTypeAndExpression)
                .hasSize(1)
                .extracting(Item::getTitle)
                .contains("Geek INC 124");
    }

    @Test
    public void should_save_an_item() {
        /* Given */
        Item item = new Item();
        item.setUrl("http://an.super.url.com/a/url");
        item.setTitle("A fake Item");
        /* When */
        Item savedItem = itemRepository.save(item);
        /* Then */
        assertThat(savedItem.getCreationDate()).isNotNull();
    }
}
