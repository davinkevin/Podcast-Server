/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables.records;


import com.github.davinkevin.podcastserver.database.tables.Podcast;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class PodcastRecord extends UpdatableRecordImpl<PodcastRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.podcast.id</code>.
     */
    public void setId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.podcast.id</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>public.podcast.description</code>.
     */
    public void setDescription(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.podcast.description</code>.
     */
    public String getDescription() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.podcast.has_to_be_deleted</code>.
     */
    public void setHasToBeDeleted(Boolean value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.podcast.has_to_be_deleted</code>.
     */
    public Boolean getHasToBeDeleted() {
        return (Boolean) get(2);
    }

    /**
     * Setter for <code>public.podcast.last_update</code>.
     */
    public void setLastUpdate(OffsetDateTime value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.podcast.last_update</code>.
     */
    public OffsetDateTime getLastUpdate() {
        return (OffsetDateTime) get(3);
    }

    /**
     * Setter for <code>public.podcast.signature</code>.
     */
    public void setSignature(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.podcast.signature</code>.
     */
    public String getSignature() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.podcast.title</code>.
     */
    public void setTitle(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.podcast.title</code>.
     */
    public String getTitle() {
        return (String) get(5);
    }

    /**
     * Setter for <code>public.podcast.type</code>.
     */
    public void setType(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>public.podcast.type</code>.
     */
    public String getType() {
        return (String) get(6);
    }

    /**
     * Setter for <code>public.podcast.url</code>.
     */
    public void setUrl(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>public.podcast.url</code>.
     */
    public String getUrl() {
        return (String) get(7);
    }

    /**
     * Setter for <code>public.podcast.cover_id</code>.
     */
    public void setCoverId(UUID value) {
        set(8, value);
    }

    /**
     * Getter for <code>public.podcast.cover_id</code>.
     */
    public UUID getCoverId() {
        return (UUID) get(8);
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
     * Create a detached PodcastRecord
     */
    public PodcastRecord() {
        super(Podcast.PODCAST);
    }

    /**
     * Create a detached, initialised PodcastRecord
     */
    public PodcastRecord(UUID id, String description, Boolean hasToBeDeleted, OffsetDateTime lastUpdate, String signature, String title, String type, String url, UUID coverId) {
        super(Podcast.PODCAST);

        setId(id);
        setDescription(description);
        setHasToBeDeleted(hasToBeDeleted);
        setLastUpdate(lastUpdate);
        setSignature(signature);
        setTitle(title);
        setType(type);
        setUrl(url);
        setCoverId(coverId);
        resetChangedOnNotNull();
    }
}
