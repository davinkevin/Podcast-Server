package com.github.davinkevin.podcastserver

import com.github.davinkevin.podcastserver.entity.Status
import com.ninja_squad.dbsetup.Operations
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import com.ninja_squad.dbsetup.operation.Operation
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

/**
 * Created by kevin on 13/03/2020
 */

val DB_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(" ").append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter()

val INSERT_TAG_DATA: Operation = sequenceOf(
        Operations.insertInto("TAG")
                .columns("ID", "NAME")
                .values(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
                .values(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
                .values(UUID.fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")
                .build()
)

val INSERT_PODCAST_DATA: Operation = sequenceOf(
        Operations.insertInto("COVER")
                .columns("ID", "URL", "WIDTH", "HEIGHT")
                .values(UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(UUID.fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                .build(),
        Operations.insertInto("PODCAST")
                .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE", "LAST_UPDATE")
                .values(UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "AppLoad", "http://fake.url.com/appload.rss", UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), false, "RSS", ZonedDateTime.now().minusDays(15).format(DB_DATE_FORMATTER))
                .values(UUID.fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD", "http://fake.url.com/geekinc.rss", UUID.fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), true, "Youtube", ZonedDateTime.now().minusDays(30).format(DB_DATE_FORMATTER))
                .build(),
        Operations.insertInto("TAG")
                .columns("ID", "NAME")
                .values(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                .values(UUID.fromString("df801a7a-5630-4442-8b83-0cb36ae94981"), "Geek")
                .values(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Renegade")
                .build(),
        Operations.insertInto("PODCAST_TAGS")
                .columns("PODCASTS_ID", "TAGS_ID")
                .values(UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                .values(UUID.fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), UUID.fromString("df801a7a-5630-4442-8b83-0cb36ae94981"))
                .values(UUID.fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                .build()
)

val INSERT_ITEM_DATA: Operation = sequenceOf(
        Operations.insertInto("COVER")
                .columns("ID", "URL", "WIDTH", "HEIGHT")
                .values(UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(UUID.fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                .build(),
        Operations.insertInto("PODCAST")
                .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                .values(UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                .values(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true)
                .build(),
        Operations.insertInto("ITEM")
                .columns("ID", "TITLE", "URL", "FILE_NAME", "PODCAST_ID", "STATUS", "PUB_DATE", "DOWNLOAD_DATE", "CREATION_DATE", "NUMBER_OF_FAIL", "COVER_ID", "DESCRIPTION", "MIME_TYPE")
                .values(UUID.fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "appload.1.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.FINISH, ZonedDateTime.now().minusDays(15).format(DB_DATE_FORMATTER), ZonedDateTime.now().minusDays(15).format(DB_DATE_FORMATTER), null, 0, UUID.fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                .values(UUID.fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "appload.2.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), null, ZonedDateTime.now().minusDays(30).format(DB_DATE_FORMATTER), null, null, 0, UUID.fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                .values(UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "appload.3.mp3", UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.NOT_DOWNLOADED, ZonedDateTime.now().format(DB_DATE_FORMATTER), null, null, 0, UUID.fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                .values(UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "geekinc.123.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.DELETED, ZonedDateTime.now().minusYears(1).format(DB_DATE_FORMATTER), ZonedDateTime.now().format(DB_DATE_FORMATTER), ZonedDateTime.now().minusMonths(2).format(DB_DATE_FORMATTER), 0, UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .values(UUID.fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "geekinc.124.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FINISH, ZonedDateTime.now().minusDays(15).format(DB_DATE_FORMATTER), ZonedDateTime.now().minusDays(15).format(DB_DATE_FORMATTER), ZonedDateTime.now().minusMonths(2).format(DB_DATE_FORMATTER), 0, UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .values(UUID.fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "geekinc.122.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, ZonedDateTime.now().minusDays(1).format(DB_DATE_FORMATTER), null, ZonedDateTime.now().minusWeeks(2).format(DB_DATE_FORMATTER), 3, UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .values(UUID.fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "geekinc.126.mp3", UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, ZonedDateTime.now().minusDays(1).format(DB_DATE_FORMATTER), null, ZonedDateTime.now().minusWeeks(1).format(DB_DATE_FORMATTER), 7, UUID.fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .build(),
        Operations.insertInto("TAG")
                .columns("ID", "NAME")
                .values(UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                .values(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere")
                .build(),
        Operations.insertInto("PODCAST_TAGS")
                .columns("PODCASTS_ID", "TAGS_ID")
                .values(UUID.fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                .values(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                .build(),
        Operations.insertInto("WATCH_LIST")
                .columns("ID", "NAME")
                .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist")
                .values(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind")
                .build(),
        Operations.insertInto("WATCH_LIST_ITEMS")
                .columns("WATCH_LISTS_ID", "ITEMS_ID")
                .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), UUID.fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), UUID.fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                .values(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"), UUID.fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                .build()
)

private val DELETE_ALL_PODCASTS: Operation = Operations.deleteAllFrom("PODCAST")
private val DELETE_ALL_ITEMS: Operation = Operations.deleteAllFrom("ITEM")
private val DELETE_ALL_TAGS = Operations.sequenceOf(Operations.deleteAllFrom("PODCAST_TAGS"), Operations.deleteAllFrom("TAG"))
private val DELETE_ALL_PLAYLIST = Operations.sequenceOf(Operations.deleteAllFrom("WATCH_LIST_ITEMS"), Operations.deleteAllFrom("WATCH_LIST"))
private val DELETE_ALL_COVERS = Operations.sequenceOf(Operations.deleteAllFrom("COVER"))

val DELETE_ALL: Operation = Operations.sequenceOf(
        DELETE_ALL_PLAYLIST,
        DELETE_ALL_ITEMS,
        DELETE_ALL_TAGS,
        DELETE_ALL_PODCASTS,
        DELETE_ALL_COVERS
)
