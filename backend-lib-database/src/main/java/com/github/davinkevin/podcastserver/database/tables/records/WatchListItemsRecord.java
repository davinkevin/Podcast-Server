/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables.records;


import com.github.davinkevin.podcastserver.database.tables.WatchListItems;

import java.util.UUID;

import org.jooq.Record2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class WatchListItemsRecord extends UpdatableRecordImpl<WatchListItemsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.watch_list_items.watch_lists_id</code>.
     */
    public void setWatchListsId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.watch_list_items.watch_lists_id</code>.
     */
    public UUID getWatchListsId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>public.watch_list_items.items_id</code>.
     */
    public void setItemsId(UUID value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.watch_list_items.items_id</code>.
     */
    public UUID getItemsId() {
        return (UUID) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<UUID, UUID> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached WatchListItemsRecord
     */
    public WatchListItemsRecord() {
        super(WatchListItems.WATCH_LIST_ITEMS);
    }

    /**
     * Create a detached, initialised WatchListItemsRecord
     */
    public WatchListItemsRecord(UUID watchListsId, UUID itemsId) {
        super(WatchListItems.WATCH_LIST_ITEMS);

        setWatchListsId(watchListsId);
        setItemsId(itemsId);
        resetChangedOnNotNull();
    }
}
