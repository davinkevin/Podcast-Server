/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables.records;


import com.github.davinkevin.podcastserver.database.tables.PodcastTags;

import java.util.UUID;

import org.jooq.Record2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PodcastTagsRecord extends UpdatableRecordImpl<PodcastTagsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.podcast_tags.podcasts_id</code>.
     */
    public void setPodcastsId(UUID value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.podcast_tags.podcasts_id</code>.
     */
    public UUID getPodcastsId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>public.podcast_tags.tags_id</code>.
     */
    public void setTagsId(UUID value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.podcast_tags.tags_id</code>.
     */
    public UUID getTagsId() {
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
     * Create a detached PodcastTagsRecord
     */
    public PodcastTagsRecord() {
        super(PodcastTags.PODCAST_TAGS);
    }

    /**
     * Create a detached, initialised PodcastTagsRecord
     */
    public PodcastTagsRecord(UUID podcastsId, UUID tagsId) {
        super(PodcastTags.PODCAST_TAGS);

        setPodcastsId(podcastsId);
        setTagsId(tagsId);
        resetChangedOnNotNull();
    }
}
