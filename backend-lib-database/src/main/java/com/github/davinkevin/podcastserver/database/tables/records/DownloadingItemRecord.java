/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables.records;


import com.github.davinkevin.podcastserver.database.enums.DownloadingState;
import com.github.davinkevin.podcastserver.database.tables.DownloadingItem;

import java.util.UUID;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DownloadingItemRecord extends UpdatableRecordImpl<DownloadingItemRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.downloading_item.item_id</code>.
     */
    public void setItemId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.downloading_item.item_id</code>.
     */
    public UUID getItemId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>public.downloading_item.position</code>.
     */
    public void setPosition(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.downloading_item.position</code>.
     */
    public Integer getPosition() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>public.downloading_item.state</code>.
     */
    public void setState(DownloadingState value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.downloading_item.state</code>.
     */
    public DownloadingState getState() {
        return (DownloadingState) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DownloadingItemRecord
     */
    public DownloadingItemRecord() {
        super(DownloadingItem.DOWNLOADING_ITEM);
    }

    /**
     * Create a detached, initialised DownloadingItemRecord
     */
    public DownloadingItemRecord(UUID itemId, Integer position, DownloadingState state) {
        super(DownloadingItem.DOWNLOADING_ITEM);

        setItemId(itemId);
        setPosition(position);
        setState(state);
        resetChangedOnNotNull();
    }
}
