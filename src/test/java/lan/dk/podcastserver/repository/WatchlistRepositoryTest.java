package lan.dk.podcastserver.repository;

import com.google.common.collect.Sets;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.entity.WatchListAssert;
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
import java.util.Set;
import java.util.UUID;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static java.time.ZonedDateTime.now;
import static lan.dk.podcastserver.repository.DatabaseConfiguraitonTest.DELETE_ALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DatabaseConfiguraitonTest.class, HibernateJpaAutoConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public class WatchListRepositoryTest {

    @Autowired DataSource dataSource;
    @Autowired WatchListRepository watchListRepository;
    @Autowired ItemRepository itemRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    public static final Operation DELETE_ALL_PLAYLIST = Operations.sequenceOf(deleteAllFrom("WATCH_LIST_ITEMS"), deleteAllFrom("WATCH_LIST"));
    public static final Operation INSERT_PLAYLIST_DATA = sequenceOf(
            ItemRepositoryTest.INSERT_ITEM_DATA,
            insertInto("WATCH_LIST")
                    .columns("ID", "NAME")
                    .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist")
                    .values(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind")
                    .build(),
            insertInto("WATCH_LIST_ITEMS")
                    .columns("WATCH_LISTS_ID", "ITEMS_ID")
                    .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), 3)
                    .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), 5)
                    .values(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"), 5)
                    .build()
    );

    @Before
    public void prepare() throws Exception {
        Operation operation = sequenceOf(DELETE_ALL, INSERT_PLAYLIST_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void should_save_a_playlist() {
        /* Given */
        WatchList watchList = WatchList.builder().name("A New Playlist").build();

        /* When */
        WatchList savedWatchList = watchListRepository.save(watchList);

        /* Then */
        assertThat(savedWatchList.getId()).isNotNull();
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        UUID id = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66");

        /* When */
        WatchList watchList = watchListRepository.findOne(id);

        /* Then */
        assertThat(watchList).isNotNull();
        assertThat(watchList.getItems()).hasSize(2);
        WatchListAssert
                .assertThat(watchList)
                .hasId(id)
                .hasName("Humour Playlist");
    }

    @Test
    public void should_find_playlist_having_item() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        Item item = new Item().setId(5);

        /* When */
        Set<WatchList> watchLists = watchListRepository.findContainsItem(item);

        /* Then */
        assertThat(watchLists).hasSize(2);
    }

    @Test
    public void should_add_a_item_to_playlist() {
        /* Given */
        Item item = itemRepository.findOne(4);
        WatchList watchList = watchListRepository.findOne(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"));

        /* When */
        watchListRepository.save(watchList.add(item));
        watchListRepository.flush();
        WatchList fetchedWatchList = watchListRepository.findOne(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"));

        /* Then */
        assertThat(fetchedWatchList.getItems()).hasSize(2);
    }

    @Test
    public void should_remove_item_from_playlist() {
        /* Given */
        Item thirdItem = itemRepository.findOne(3);
        WatchList watchList = watchListRepository.findOne(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"));

        /* When */
        watchListRepository.save(watchList.remove(thirdItem));
        watchListRepository.flush();
        WatchList fetchedWatchList = watchListRepository.findOne(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"));

        /* Then */
        assertThat(fetchedWatchList.getItems()).hasSize(1);
    }

    @Test
    public void should_remove_from_playlist_if_item_deleted() {
        /* Given */

        /* When */
        itemRepository.delete(3);
        itemRepository.flush();
        WatchList fetchedWatchList = watchListRepository.findOne(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"));

        /* Then */
        assertThat(fetchedWatchList.getItems()).hasSize(1);
    }

    @Test
    public void should_find_all_to_delete() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        /* When */
        Set<Item> allToDelete = Sets.newHashSet(itemRepository.findAllToDelete(now()));
        /* Then */
        assertThat(allToDelete).isEmpty();
    }




}