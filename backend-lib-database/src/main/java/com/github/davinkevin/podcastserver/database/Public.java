/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database;


import com.github.davinkevin.podcastserver.database.tables.Cover;
import com.github.davinkevin.podcastserver.database.tables.DownloadingItem;
import com.github.davinkevin.podcastserver.database.tables.FlywaySchemaHistory;
import com.github.davinkevin.podcastserver.database.tables.Item;
import com.github.davinkevin.podcastserver.database.tables.Podcast;
import com.github.davinkevin.podcastserver.database.tables.PodcastTags;
import com.github.davinkevin.podcastserver.database.tables.Tag;
import com.github.davinkevin.podcastserver.database.tables.WatchList;
import com.github.davinkevin.podcastserver.database.tables.WatchListItems;

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.cover</code>.
     */
    public final Cover COVER = Cover.COVER;

    /**
     * The table <code>public.downloading_item</code>.
     */
    public final DownloadingItem DOWNLOADING_ITEM = DownloadingItem.DOWNLOADING_ITEM;

    /**
     * The table <code>public.flyway_schema_history</code>.
     */
    public final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>public.item</code>.
     */
    public final Item ITEM = Item.ITEM;

    /**
     * The table <code>public.podcast</code>.
     */
    public final Podcast PODCAST = Podcast.PODCAST;

    /**
     * The table <code>public.podcast_tags</code>.
     */
    public final PodcastTags PODCAST_TAGS = PodcastTags.PODCAST_TAGS;

    /**
     * The table <code>public.tag</code>.
     */
    public final Tag TAG = Tag.TAG;

    /**
     * The table <code>public.watch_list</code>.
     */
    public final WatchList WATCH_LIST = WatchList.WATCH_LIST;

    /**
     * The table <code>public.watch_list_items</code>.
     */
    public final WatchListItems WATCH_LIST_ITEMS = WatchListItems.WATCH_LIST_ITEMS;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Cover.COVER,
            DownloadingItem.DOWNLOADING_ITEM,
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            Item.ITEM,
            Podcast.PODCAST,
            PodcastTags.PODCAST_TAGS,
            Tag.TAG,
            WatchList.WATCH_LIST,
            WatchListItems.WATCH_LIST_ITEMS
        );
    }
}
