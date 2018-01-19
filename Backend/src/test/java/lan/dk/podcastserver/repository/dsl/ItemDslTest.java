package lan.dk.podcastserver.repository.dsl;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import com.querydsl.core.types.ExpressionUtils;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.QItem;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.DatabaseConfigurationTest;
import lan.dk.podcastserver.repository.ItemRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.stream.Stream;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static io.vavr.API.Set;
import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL;
import static lan.dk.podcastserver.repository.DatabaseConfigurationTest.formatter;
import static lan.dk.podcastserver.repository.dsl.ItemDSL.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DatabaseConfigurationTest.class})
public class ItemDslTest {

    @Autowired DataSource dataSource;
    @Autowired ItemRepository itemRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    private static final Operation INSERT_REFERENCE_DATA = sequenceOf(
            insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                    .values(UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true)
                    .build(),
            insertInto("ITEM")
                    .columns("ID", "TITLE", "URL", "PODCAST_ID", "STATUS", "PUB_DATE", "DOWNLOAD_DATE")
                    .values(UUID.fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter))
                    .values(UUID.fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), null, now().minusDays(30).format(formatter), now().minusDays(30).format(formatter))
                    .values(UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.NOT_DOWNLOADED, now().format(formatter), now().format(formatter))
                    .values(UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.DELETED, now().minusYears(1).format(formatter), now().format(formatter))
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

    @Test
    public void should_find_downloaded() {
        /* When */
        dbSetupTracker.skipNextLaunch();
        Iterable<Item> one = itemRepository.findAll(isFinished());

        /* Then */
        assertThat(one)
                .hasSize(1)
                .extracting(Item::getTitle)
                .contains("Appload 1");
    }

    @Test
    public void should_find_not_downloaded() {
        /* When */
        dbSetupTracker.skipNextLaunch();
        Iterable<Item> items = itemRepository.findAll(isNotDownloaded());

        /* Then */
        assertThat(items)
                .hasSize(2)
                .extracting(Item::getTitle)
                .contains("Appload 2", "Appload 3");
    }

    @Test
    public void should_find_newer_than_16_days() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Iterable<Item> items = itemRepository.findAll(isNewerThan(now().minusDays(16)));

        /* Then */
        assertThat(items)
                .hasSize(2)
                .extracting(Item::getTitle)
                .contains("Appload 1", "Appload 3");
    }

    @Test
    public void should_find_downloaded_newer_than_16_days() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Iterable<Item> items = itemRepository.findAll(hasBeenDownloadedBefore(now().minusDays(16)));

        /* Then */
        assertThat(items)
                .hasSize(1)
                .extracting(Item::getTitle)
                .contains("Appload 2");
    }

    @Test
    public void should_find_downloaded_older_than_16_days() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Iterable<Item> items = itemRepository.findAll(hasBeenDownloadedAfter(now().minusDays(16)));

        /* Then */
        assertThat(items)
                .hasSize(3)
                .extracting(Item::getTitle)
                .contains("Appload 1", "Appload 3", "Geek INC 123");
    }

    @Test
    public void should_find_item_from_podcast_of_type_YOUTUBE() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Iterable<Item> items = itemRepository.findAll(isOfType("YOUTUBE"));

        /* Then */
        assertThat(items)
                .hasSize(1)
                .extracting(Item::getTitle)
                .contains("Geek INC 123");
    }
    
    @Test
    public void should_find_item_witch_has_to_be_deleted() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Iterable<Item> items = itemRepository.findAll(hasToBeDeleted(Boolean.FALSE));

        /* Then */
        assertThat(items)
                .hasSize(3)
                .extracting(Item::getTitle)
                .contains("Appload 1", "Appload 2", "Appload 3");
    }

    @Test
    public void should_be_in_id_list() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        io.vavr.collection.List<UUID> listOfId = io.vavr.collection.List.of(UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"));

        /* When */
        Iterable<Item> items = itemRepository.findAll(isInId(listOfId));

        /* Then */
        assertThat(items)
                .hasSize(2)
                .extracting(Item::getTitle)
                .contains("Appload 3", "Geek INC 123");
    }

    @Test
    public void should_find_in_tag() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        Tag tag = new Tag().setId(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08")).setName("French Spin");


        /* When */
        Iterable<Item> items = itemRepository.findAll(ExpressionUtils.allOf(Stream.of(new Tag[]{tag})
                .map(QItem.item.podcast.tags::contains)
                .collect(toList())
        ));

        /* Then */
        assertThat(items)
                .hasSize(3)
                .extracting(Item::getTitle)
                .contains("Appload 1", "Appload 2", "Appload 3");
    }

    @Test
    public void should_be_in_podcast() {
        /* Given */
        dbSetupTracker.skipNextLaunch();

        /* When */
        Iterable<Item> items = itemRepository.findAll(isInPodcast(UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561")));

        /* Then */
        assertThat(items)
                .hasSize(3)
                .extracting(Item::getTitle)
                .contains("Appload 1", "Appload 2", "Appload 3");
    }

    @Test
    public void should_result_with_search() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        io.vavr.collection.List<UUID> ids = io.vavr.collection.List.of(UUID.fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"));
        io.vavr.collection.Set<Tag> tags = io.vavr.collection.HashSet.of(new Tag().setId(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")).setName("Tag1"));

        /* When */
        Iterable<Item> items = itemRepository.findAll(getSearchSpecifications(ids, tags, Set()));

        /* Then */
        assertThat(items)
                .hasSize(1)
                .extracting(Item::getTitle)
                .contains("Geek INC 123");
    }

    @Test
    public void should_result_with_search_without_ids() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        io.vavr.collection.Set<Tag> tags = io.vavr.collection.HashSet.of(new Tag().setId(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")).setName("Tag1"));

        /* When */
        Iterable<Item> items = itemRepository.findAll(getSearchSpecifications(null, tags, Set()));

        /* Then */
        assertThat(items)
                .hasSize(1)
                .extracting(Item::getTitle)
                .contains("Geek INC 123");
    }

    @Test(expected = InvocationTargetException.class)
    public void should_throw_exception_on_dsl_instanciation() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<ItemDSL> constructor = ItemDSL.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }



    @Before
    public void prepare() throws Exception {
        Operation operation = sequenceOf(DELETE_ALL, INSERT_REFERENCE_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

}
