/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database;


import com.github.davinkevin.podcastserver.database.tables.Cover;
import com.github.davinkevin.podcastserver.database.tables.DownloadingItem;
import com.github.davinkevin.podcastserver.database.tables.FlywaySchemaHistory;
import com.github.davinkevin.podcastserver.database.tables.Item;
import com.github.davinkevin.podcastserver.database.tables.Playlist;
import com.github.davinkevin.podcastserver.database.tables.PlaylistItems;
import com.github.davinkevin.podcastserver.database.tables.Podcast;
import com.github.davinkevin.podcastserver.database.tables.PodcastTags;
import com.github.davinkevin.podcastserver.database.tables.Tag;
import com.github.davinkevin.podcastserver.database.tables.records.CoverRecord;
import com.github.davinkevin.podcastserver.database.tables.records.DownloadingItemRecord;
import com.github.davinkevin.podcastserver.database.tables.records.FlywaySchemaHistoryRecord;
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord;
import com.github.davinkevin.podcastserver.database.tables.records.PlaylistItemsRecord;
import com.github.davinkevin.podcastserver.database.tables.records.PlaylistRecord;
import com.github.davinkevin.podcastserver.database.tables.records.PodcastRecord;
import com.github.davinkevin.podcastserver.database.tables.records.PodcastTagsRecord;
import com.github.davinkevin.podcastserver.database.tables.records.TagRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * public.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<CoverRecord> COVER_PKEY = Internal.createUniqueKey(Cover.COVER, DSL.name("cover_pkey"), new TableField[] { Cover.COVER.ID }, true);
    public static final UniqueKey<DownloadingItemRecord> DOWNLOADING_ITEM_PKEY = Internal.createUniqueKey(DownloadingItem.DOWNLOADING_ITEM, DSL.name("downloading_item_pkey"), new TableField[] { DownloadingItem.DOWNLOADING_ITEM.ITEM_ID }, true);
    public static final UniqueKey<DownloadingItemRecord> DOWNLOADING_ITEM_POSITION_KEY = Internal.createUniqueKey(DownloadingItem.DOWNLOADING_ITEM, DSL.name("downloading_item_position_key"), new TableField[] { DownloadingItem.DOWNLOADING_ITEM.POSITION }, true);
    public static final UniqueKey<FlywaySchemaHistoryRecord> FLYWAY_SCHEMA_HISTORY_PK = Internal.createUniqueKey(FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, DSL.name("flyway_schema_history_pk"), new TableField[] { FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK }, true);
    public static final UniqueKey<ItemRecord> ITEM_PKEY = Internal.createUniqueKey(Item.ITEM, DSL.name("item_pkey"), new TableField[] { Item.ITEM.ID }, true);
    public static final UniqueKey<ItemRecord> ITEM_WITH_GUID_IS_UNIQUE_IN_PODCAST = Internal.createUniqueKey(Item.ITEM, DSL.name("item_with_guid_is_unique_in_podcast"), new TableField[] { Item.ITEM.GUID, Item.ITEM.PODCAST_ID }, true);
    public static final UniqueKey<PlaylistRecord> PLAYLIST_NAME_KEY = Internal.createUniqueKey(Playlist.PLAYLIST, DSL.name("playlist_name_key"), new TableField[] { Playlist.PLAYLIST.NAME }, true);
    public static final UniqueKey<PlaylistRecord> PLAYLIST_PKEY = Internal.createUniqueKey(Playlist.PLAYLIST, DSL.name("playlist_pkey"), new TableField[] { Playlist.PLAYLIST.ID }, true);
    public static final UniqueKey<PlaylistItemsRecord> PLAYLIST_ITEMS_PKEY = Internal.createUniqueKey(PlaylistItems.PLAYLIST_ITEMS, DSL.name("playlist_items_pkey"), new TableField[] { PlaylistItems.PLAYLIST_ITEMS.PLAYLISTS_ID, PlaylistItems.PLAYLIST_ITEMS.ITEMS_ID }, true);
    public static final UniqueKey<PodcastRecord> PODCAST_PKEY = Internal.createUniqueKey(Podcast.PODCAST, DSL.name("podcast_pkey"), new TableField[] { Podcast.PODCAST.ID }, true);
    public static final UniqueKey<PodcastRecord> PODCAST_URL_KEY = Internal.createUniqueKey(Podcast.PODCAST, DSL.name("podcast_url_key"), new TableField[] { Podcast.PODCAST.URL }, true);
    public static final UniqueKey<PodcastTagsRecord> PODCAST_TAGS_PKEY = Internal.createUniqueKey(PodcastTags.PODCAST_TAGS, DSL.name("podcast_tags_pkey"), new TableField[] { PodcastTags.PODCAST_TAGS.PODCASTS_ID, PodcastTags.PODCAST_TAGS.TAGS_ID }, true);
    public static final UniqueKey<TagRecord> TAG_NAME_KEY = Internal.createUniqueKey(Tag.TAG, DSL.name("tag_name_key"), new TableField[] { Tag.TAG.NAME }, true);
    public static final UniqueKey<TagRecord> TAG_PKEY = Internal.createUniqueKey(Tag.TAG, DSL.name("tag_pkey"), new TableField[] { Tag.TAG.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<DownloadingItemRecord, ItemRecord> DOWNLOADING_ITEM__DOWNLOADING_ITEM_ITEM_ID_FKEY = Internal.createForeignKey(DownloadingItem.DOWNLOADING_ITEM, DSL.name("downloading_item_item_id_fkey"), new TableField[] { DownloadingItem.DOWNLOADING_ITEM.ITEM_ID }, Keys.ITEM_PKEY, new TableField[] { Item.ITEM.ID }, true);
    public static final ForeignKey<ItemRecord, CoverRecord> ITEM__ITEM_COVER_ID_FKEY = Internal.createForeignKey(Item.ITEM, DSL.name("item_cover_id_fkey"), new TableField[] { Item.ITEM.COVER_ID }, Keys.COVER_PKEY, new TableField[] { Cover.COVER.ID }, true);
    public static final ForeignKey<ItemRecord, PodcastRecord> ITEM__ITEM_PODCAST_ID_FKEY = Internal.createForeignKey(Item.ITEM, DSL.name("item_podcast_id_fkey"), new TableField[] { Item.ITEM.PODCAST_ID }, Keys.PODCAST_PKEY, new TableField[] { Podcast.PODCAST.ID }, true);
    public static final ForeignKey<PlaylistRecord, CoverRecord> PLAYLIST__PLAYLIST_COVER_ID_FK = Internal.createForeignKey(Playlist.PLAYLIST, DSL.name("playlist_cover_id_fk"), new TableField[] { Playlist.PLAYLIST.COVER_ID }, Keys.COVER_PKEY, new TableField[] { Cover.COVER.ID }, true);
    public static final ForeignKey<PlaylistItemsRecord, ItemRecord> PLAYLIST_ITEMS__PLAYLIST_ITEMS_ITEMS_ID_FKEY = Internal.createForeignKey(PlaylistItems.PLAYLIST_ITEMS, DSL.name("playlist_items_items_id_fkey"), new TableField[] { PlaylistItems.PLAYLIST_ITEMS.ITEMS_ID }, Keys.ITEM_PKEY, new TableField[] { Item.ITEM.ID }, true);
    public static final ForeignKey<PlaylistItemsRecord, PlaylistRecord> PLAYLIST_ITEMS__PLAYLIST_ITEMS_WATCH_LISTS_ID_FKEY = Internal.createForeignKey(PlaylistItems.PLAYLIST_ITEMS, DSL.name("playlist_items_watch_lists_id_fkey"), new TableField[] { PlaylistItems.PLAYLIST_ITEMS.PLAYLISTS_ID }, Keys.PLAYLIST_PKEY, new TableField[] { Playlist.PLAYLIST.ID }, true);
    public static final ForeignKey<PodcastRecord, CoverRecord> PODCAST__PODCAST_COVER_ID_FKEY = Internal.createForeignKey(Podcast.PODCAST, DSL.name("podcast_cover_id_fkey"), new TableField[] { Podcast.PODCAST.COVER_ID }, Keys.COVER_PKEY, new TableField[] { Cover.COVER.ID }, true);
    public static final ForeignKey<PodcastTagsRecord, PodcastRecord> PODCAST_TAGS__PODCAST_TAGS_PODCASTS_ID_FKEY = Internal.createForeignKey(PodcastTags.PODCAST_TAGS, DSL.name("podcast_tags_podcasts_id_fkey"), new TableField[] { PodcastTags.PODCAST_TAGS.PODCASTS_ID }, Keys.PODCAST_PKEY, new TableField[] { Podcast.PODCAST.ID }, true);
    public static final ForeignKey<PodcastTagsRecord, TagRecord> PODCAST_TAGS__PODCAST_TAGS_TAGS_ID_FKEY = Internal.createForeignKey(PodcastTags.PODCAST_TAGS, DSL.name("podcast_tags_tags_id_fkey"), new TableField[] { PodcastTags.PODCAST_TAGS.TAGS_ID }, Keys.TAG_PKEY, new TableField[] { Tag.TAG.ID }, true);
}
