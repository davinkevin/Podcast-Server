package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Status;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.operation.CompositeOperation;
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
import java.util.UUID;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.fromString;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@Configuration
@EnableJpaRepositories(basePackages = "lan.dk.podcastserver.repository")
@EntityScan(basePackages = {"lan.dk.podcastserver.entity", "com.github.davinkevin.podcastserver.entity"})
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class DatabaseConfigurationTest {

    private static final Operation DELETE_ALL_PODCASTS = deleteAllFrom("PODCAST");
    private static final Operation DELETE_ALL_ITEMS = deleteAllFrom("ITEM");
    private static final Operation DELETE_ALL_TAGS = sequenceOf(deleteAllFrom("PODCAST_TAGS"), deleteAllFrom("TAG"));
    private static final Operation DELETE_ALL_PLAYLIST = Operations.sequenceOf(deleteAllFrom("WATCH_LIST_ITEMS"), deleteAllFrom("WATCH_LIST"));
    private static final Operation DELETE_ALL_COVERS = Operations.sequenceOf(deleteAllFrom("COVER"));

    public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(" ").append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter();
    public static final Operation DELETE_ALL = sequenceOf(
            DELETE_ALL_PLAYLIST,
            DELETE_ALL_ITEMS,
            DELETE_ALL_TAGS,
            DELETE_ALL_PODCASTS,
            DELETE_ALL_COVERS
    );

    @Bean DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(now());
    }

    public static final Operation INSERT_ITEM_DATA = CompositeOperation.sequenceOf(
            insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                    .build(),
            insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true)
                    .build(),
            insertInto("ITEM")
                    .columns("ID", "TITLE", "URL", "FILE_NAME", "PODCAST_ID", "STATUS", "PUB_DATE", "DOWNLOAD_DATE", "CREATION_DATE", "NUMBER_OF_FAIL", "COVER_ID", "DESCRIPTION")
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "appload.1.mp3", fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "appload.2.mp3", fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), null, now().minusDays(30).format(formatter), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "appload.3.mp3", fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.NOT_DOWNLOADED, now().format(formatter), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "geekinc.123.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.DELETED, now().minusYears(1).format(formatter), now().format(formatter), now().minusMonths(2).format(formatter), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "geekinc.124.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter), now().minusMonths(2).format(formatter), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "geekinc.122.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, now().minusDays(1).format(formatter), null, now().minusWeeks(2).format(formatter), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "geekinc.126.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, now().minusDays(1).format(formatter), null, now().minusWeeks(1).format(formatter), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc")
                    .build(),
            insertInto("TAG")
                    .columns("ID", "NAME")
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere")
                    .build(),
            insertInto("PODCAST_TAGS")
                    .columns("PODCASTS_ID", "TAGS_ID")
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                    .build()
    );

    public static final Operation INSERT_PODCAST_DATA = CompositeOperation.sequenceOf(
            insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                    .build(),
            insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "COVER_ID")
                    .values(UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "AppLoad", "http://fake.url.com/rss", fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"))
                    .values(UUID.fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD", "http://fake.url.com/rss", fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"))
                    .build()
    );

    public static final Operation INSERT_TAG_DATA = CompositeOperation.sequenceOf(
            insertInto("TAG")
                    .columns("ID", "NAME")
                    .values(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
                    .values(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
                    .values(UUID.fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")
                    .build()
    );
}
