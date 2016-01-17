package lan.dk.podcastserver.repository;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.ItemAssert;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageAssert;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.time.ZonedDateTime;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static java.time.ZonedDateTime.now;
import static lan.dk.podcastserver.repository.DatabaseConfiguraitonTest.DELETE_ALL;
import static lan.dk.podcastserver.repository.DatabaseConfiguraitonTest.formatter;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 04/09/15 for Podcast Server
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DatabaseConfiguraitonTest.class, HibernateJpaAutoConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public class ItemRepositoryTest {

    @Autowired DataSource dataSource;
    @Autowired ItemRepository itemRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    public static final Operation INSERT_ITEM_DATA = sequenceOf(
            insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                    .values(1, "AppLoad", null, "RSS", false)
                    .values(2, "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true)
                    .build(),
            insertInto("ITEM")
                    .columns("ID", "TITLE", "URL", "PODCAST_ID", "STATUS", "PUBDATE", "DOWNLOAD_DATE")
                    .values(1L, "Appload 1", "http://fakeurl.com/appload.1.mp3", 1, Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter))
                    .values(2L, "Appload 2", "http://fakeurl.com/appload.2.mp3", 1, null, now().minusDays(30).format(formatter), null)
                    .values(3L, "Appload 3", "http://fakeurl.com/appload.3.mp3", 1, Status.NOT_DOWNLOADED, now().format(formatter), null)
                    .values(4L, "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", 2, Status.DELETED, now().minusYears(1).format(formatter), now().format(formatter))
                    .values(5L, "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", 2, Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter))
                    .build(),
            insertInto("TAG")
                    .columns("ID", "NAME")
                    .values(1L, "French Spin")
                    .values(2L, "Studio Knowhere")
                    .build(),
            insertInto("PODCAST_TAGS")
                    .columns("PODCASTS_ID", "TAGS_ID")
                    .values(1, 1)
                    .values(2, 2)
                    .build()
    );

    @Before
    public void prepare() throws Exception {
        Operation operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void should_find_by_podcast_and_page() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        Integer podcastId = 1;
        PageRequest pageRequest = new PageRequest(1, 1, Sort.Direction.ASC, "id");

        /* When */
        Page<Item> itemByPodcast = itemRepository.findByPodcast(podcastId, pageRequest);

        /* Then */
        PageAssert
                .assertThat(itemByPodcast)
                .hasSize(1)
                .hasTotalElements(3)
                .hasTotalPages(3)
                .hasNumberOfElements(1);

        ItemAssert
                .assertThat(itemByPodcast.getContent().get(0))
                .hasTitle("Appload 2");
    }

    @Test
    public void should_find_all_to_download() {
        dbSetupTracker.skipNextLaunch();
        /* Given */ ZonedDateTime date = now().minusDays(15);
        /* When */  Iterable<Item> itemToDownload = itemRepository.findAllToDownload(date);
        /* Then */
        assertThat(itemToDownload)
                .hasSize(1);
        ItemAssert.assertThat(itemToDownload.iterator().next())
                .hasTitle("Appload 3");
    }
    
    @Test
    public void should_find_all_to_delete() {
        dbSetupTracker.skipNextLaunch();
        /* Given */ ZonedDateTime today = now();
        /* When */ Iterable<Item> itemToDelete = itemRepository.findAllToDelete(today);
        /* Then */
        assertThat(itemToDelete)
                .hasSize(1);
        ItemAssert.assertThat(itemToDelete.iterator().next())
                .hasTitle("Geek INC 124");
    }

    @Test
    public void should_find_by_type_and_downloaded_after() {
        dbSetupTracker.skipNextLaunch();
        /* Given */
        AbstractUpdater.Type type = new AbstractUpdater.Type("YOUTUBE", "Youtube");
        ZonedDateTime date = now().minusDays(60);

        /* When */
        Iterable<Item> itemByTypeAndDownloadAfter = itemRepository.findByTypeAndDownloadDateAfter(type, date);

        /* Then */
        assertThat(itemByTypeAndDownloadAfter)
                .hasSize(2)
                .extracting("title")
                .containsOnly("Geek INC 123", "Geek INC 124");
    }

    @Test
    public void should_find_by_status() {
        dbSetupTracker.skipNextLaunch();
        /* Given */
        /* When */ Iterable<Item> itemByStatus = itemRepository.findByStatus(Status.FINISH, Status.DELETED);
        /* Then */
        assertThat(itemByStatus)
                .hasSize(3)
                .extracting("title")
                .containsOnly("Geek INC 124", "Geek INC 123", "Appload 1");
    }

    @Test
    public void should_save_an_item() {
        /* Given */
        Item item = new Item().setUrl("http://an.super.url.com/a/url").setTitle("A fake Item");
        /* When */
        Item savedItem = itemRepository.save(item);
        /* Then */
        assertThat(savedItem.getCreationDate()).isNotNull();
    }
}
