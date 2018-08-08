package lan.dk.podcastserver.repository;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import io.vavr.API;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageAssert;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static java.time.ZonedDateTime.now;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.formatter;
import static lan.dk.podcastserver.repository.dsl.ItemDSL.hasStatus;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 04/09/15 for Podcast Server
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DatabaseConfigurationTest.class})
public class ItemRepositoryTest {

    @Autowired DataSource dataSource;
    @Autowired ItemRepository itemRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    public static final Operation INSERT_ITEM_DATA = sequenceOf(
            insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                    .values(UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true)
                    .build(),
            insertInto("ITEM")
                    .columns("ID", "TITLE", "URL", "PODCAST_ID", "STATUS", "PUB_DATE", "DOWNLOAD_DATE", "NUMBER_OF_FAIL")
                    .values(UUID.fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter), 0)
                    .values(UUID.fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), null, now().minusDays(30).format(formatter), null, 0)
                    .values(UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.NOT_DOWNLOADED, now().format(formatter), null, 0)
                    .values(UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.DELETED, now().minusYears(1).format(formatter), now().format(formatter), 0)
                    .values(UUID.fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter), 0)
                    .values(UUID.fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, now().minusDays(1).format(formatter), null, 3)
                    .values(UUID.fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, now().minusDays(1).format(formatter), null, 7)
                    .build(),
            insertInto("TAG")
                    .columns("ID", "NAME")
                    .values(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere")
                    .build(),
            insertInto("PODCAST_TAGS")
                    .columns("PODCASTS_ID", "TAGS_ID")
                    .values(UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
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
        UUID podcastId = UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561");
        PageRequest pageRequest = PageRequest.of(1, 1, Sort.Direction.ASC, "id");

        /* When */
        Page<Item> itemByPodcast = itemRepository.findByPodcast(podcastId, pageRequest);

        /* Then */
        PageAssert
                .assertThat(itemByPodcast)
                .hasSize(1)
                .hasTotalElements(3)
                .hasTotalPages(3)
                .hasNumberOfElements(1);

        assertThat(itemByPodcast.getContent().get(0)).hasTitle("Appload 1");
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
    public void should_find_all_to_delete() {
        dbSetupTracker.skipNextLaunch();
        /* Given */
        ZonedDateTime today = now();

        /* When */
        Set<Item> itemToDelete = itemRepository.findAllToDelete(today);

        /* Then */
        assertThat(itemToDelete).hasSize(1);
        assertThat(itemToDelete.get()).hasTitle("Geek INC 124");
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
        Item item = new Item().setUrl("http://an.super.url.com/a/url").setTitle("A fake Item");
        /* When */
        Item savedItem = itemRepository.save(item);
        /* Then */
        assertThat(savedItem.getCreationDate()).isNotNull();
    }
}
