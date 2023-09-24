/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables.records;


import com.github.davinkevin.podcastserver.database.tables.Podcast;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PodcastRecord extends UpdatableRecordImpl<PodcastRecord> implements Record9<UUID, String, Boolean, OffsetDateTime, String, String, String, String, UUID> {

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
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<UUID, String, Boolean, OffsetDateTime, String, String, String, String, UUID> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<UUID, String, Boolean, OffsetDateTime, String, String, String, String, UUID> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return Podcast.PODCAST.ID;
    }

    @Override
    public Field<String> field2() {
        return Podcast.PODCAST.DESCRIPTION;
    }

    @Override
    public Field<Boolean> field3() {
        return Podcast.PODCAST.HAS_TO_BE_DELETED;
    }

    @Override
    public Field<OffsetDateTime> field4() {
        return Podcast.PODCAST.LAST_UPDATE;
    }

    @Override
    public Field<String> field5() {
        return Podcast.PODCAST.SIGNATURE;
    }

    @Override
    public Field<String> field6() {
        return Podcast.PODCAST.TITLE;
    }

    @Override
    public Field<String> field7() {
        return Podcast.PODCAST.TYPE;
    }

    @Override
    public Field<String> field8() {
        return Podcast.PODCAST.URL;
    }

    @Override
    public Field<UUID> field9() {
        return Podcast.PODCAST.COVER_ID;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getDescription();
    }

    @Override
    public Boolean component3() {
        return getHasToBeDeleted();
    }

    @Override
    public OffsetDateTime component4() {
        return getLastUpdate();
    }

    @Override
    public String component5() {
        return getSignature();
    }

    @Override
    public String component6() {
        return getTitle();
    }

    @Override
    public String component7() {
        return getType();
    }

    @Override
    public String component8() {
        return getUrl();
    }

    @Override
    public UUID component9() {
        return getCoverId();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getDescription();
    }

    @Override
    public Boolean value3() {
        return getHasToBeDeleted();
    }

    @Override
    public OffsetDateTime value4() {
        return getLastUpdate();
    }

    @Override
    public String value5() {
        return getSignature();
    }

    @Override
    public String value6() {
        return getTitle();
    }

    @Override
    public String value7() {
        return getType();
    }

    @Override
    public String value8() {
        return getUrl();
    }

    @Override
    public UUID value9() {
        return getCoverId();
    }

    @Override
    public PodcastRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public PodcastRecord value2(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public PodcastRecord value3(Boolean value) {
        setHasToBeDeleted(value);
        return this;
    }

    @Override
    public PodcastRecord value4(OffsetDateTime value) {
        setLastUpdate(value);
        return this;
    }

    @Override
    public PodcastRecord value5(String value) {
        setSignature(value);
        return this;
    }

    @Override
    public PodcastRecord value6(String value) {
        setTitle(value);
        return this;
    }

    @Override
    public PodcastRecord value7(String value) {
        setType(value);
        return this;
    }

    @Override
    public PodcastRecord value8(String value) {
        setUrl(value);
        return this;
    }

    @Override
    public PodcastRecord value9(UUID value) {
        setCoverId(value);
        return this;
    }

    @Override
    public PodcastRecord values(UUID value1, String value2, Boolean value3, OffsetDateTime value4, String value5, String value6, String value7, String value8, UUID value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
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