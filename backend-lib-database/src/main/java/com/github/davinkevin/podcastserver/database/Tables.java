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


/**
 * Convenience access to all tables in public.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>public.cover</code>.
     */
    public static final Cover COVER = Cover.COVER;

    /**
     * The table <code>public.downloading_item</code>.
     */
    public static final DownloadingItem DOWNLOADING_ITEM = DownloadingItem.DOWNLOADING_ITEM;

    /**
     * The table <code>public.flyway_schema_history</code>.
     */
    public static final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>public.item</code>.
     */
    public static final Item ITEM = Item.ITEM;

    /**
     * The table <code>public.playlist</code>.
     */
    public static final Playlist PLAYLIST = Playlist.PLAYLIST;

    /**
     * The table <code>public.playlist_items</code>.
     */
    public static final PlaylistItems PLAYLIST_ITEMS = PlaylistItems.PLAYLIST_ITEMS;

    /**
     * The table <code>public.podcast</code>.
     */
    public static final Podcast PODCAST = Podcast.PODCAST;

    /**
     * The table <code>public.podcast_tags</code>.
     */
    public static final PodcastTags PODCAST_TAGS = PodcastTags.PODCAST_TAGS;

    /**
     * The table <code>public.tag</code>.
     */
    public static final Tag TAG = Tag.TAG;
}
