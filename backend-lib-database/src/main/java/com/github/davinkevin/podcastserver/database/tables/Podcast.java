/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables;


import com.github.davinkevin.podcastserver.database.Keys;
import com.github.davinkevin.podcastserver.database.Public;
import com.github.davinkevin.podcastserver.database.tables.Cover.CoverPath;
import com.github.davinkevin.podcastserver.database.tables.Item.ItemPath;
import com.github.davinkevin.podcastserver.database.tables.PodcastTags.PodcastTagsPath;
import com.github.davinkevin.podcastserver.database.tables.Tag.TagPath;
import com.github.davinkevin.podcastserver.database.tables.records.PodcastRecord;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.InverseForeignKey;
import org.jooq.Name;
import org.jooq.Path;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.Record;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Podcast extends TableImpl<PodcastRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.podcast</code>
     */
    public static final Podcast PODCAST = new Podcast();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PodcastRecord> getRecordType() {
        return PodcastRecord.class;
    }

    /**
     * The column <code>public.podcast.id</code>.
     */
    public final TableField<PodcastRecord, UUID> ID = createField(DSL.name("id"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>public.podcast.description</code>.
     */
    public final TableField<PodcastRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR(65535), this, "");

    /**
     * The column <code>public.podcast.has_to_be_deleted</code>.
     */
    public final TableField<PodcastRecord, Boolean> HAS_TO_BE_DELETED = createField(DSL.name("has_to_be_deleted"), SQLDataType.BOOLEAN, this, "");

    /**
     * The column <code>public.podcast.last_update</code>.
     */
    public final TableField<PodcastRecord, OffsetDateTime> LAST_UPDATE = createField(DSL.name("last_update"), SQLDataType.TIMESTAMPWITHTIMEZONE(6), this, "");

    /**
     * The column <code>public.podcast.signature</code>.
     */
    public final TableField<PodcastRecord, String> SIGNATURE = createField(DSL.name("signature"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>public.podcast.title</code>.
     */
    public final TableField<PodcastRecord, String> TITLE = createField(DSL.name("title"), SQLDataType.VARCHAR(65535), this, "");

    /**
     * The column <code>public.podcast.type</code>.
     */
    public final TableField<PodcastRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>public.podcast.url</code>.
     */
    public final TableField<PodcastRecord, String> URL = createField(DSL.name("url"), SQLDataType.VARCHAR(65535), this, "");

    /**
     * The column <code>public.podcast.cover_id</code>.
     */
    public final TableField<PodcastRecord, UUID> COVER_ID = createField(DSL.name("cover_id"), SQLDataType.UUID, this, "");

    private Podcast(Name alias, Table<PodcastRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private Podcast(Name alias, Table<PodcastRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.podcast</code> table reference
     */
    public Podcast(String alias) {
        this(DSL.name(alias), PODCAST);
    }

    /**
     * Create an aliased <code>public.podcast</code> table reference
     */
    public Podcast(Name alias) {
        this(alias, PODCAST);
    }

    /**
     * Create a <code>public.podcast</code> table reference
     */
    public Podcast() {
        this(DSL.name("podcast"), null);
    }

    public <O extends Record> Podcast(Table<O> path, ForeignKey<O, PodcastRecord> childPath, InverseForeignKey<O, PodcastRecord> parentPath) {
        super(path, childPath, parentPath, PODCAST);
    }

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    public static class PodcastPath extends Podcast implements Path<PodcastRecord> {

        private static final long serialVersionUID = 1L;
        public <O extends Record> PodcastPath(Table<O> path, ForeignKey<O, PodcastRecord> childPath, InverseForeignKey<O, PodcastRecord> parentPath) {
            super(path, childPath, parentPath);
        }
        private PodcastPath(Name alias, Table<PodcastRecord> aliased) {
            super(alias, aliased);
        }

        @Override
        public PodcastPath as(String alias) {
            return new PodcastPath(DSL.name(alias), this);
        }

        @Override
        public PodcastPath as(Name alias) {
            return new PodcastPath(alias, this);
        }

        @Override
        public PodcastPath as(Table<?> alias) {
            return new PodcastPath(alias.getQualifiedName(), this);
        }
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<PodcastRecord> getPrimaryKey() {
        return Keys.PODCAST_PKEY;
    }

    @Override
    public List<UniqueKey<PodcastRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.PODCAST_URL_KEY);
    }

    @Override
    public List<ForeignKey<PodcastRecord, ?>> getReferences() {
        return Arrays.asList(Keys.PODCAST__PODCAST_COVER_ID_FKEY);
    }

    private transient CoverPath _cover;

    /**
     * Get the implicit join path to the <code>public.cover</code> table.
     */
    public CoverPath cover() {
        if (_cover == null)
            _cover = new CoverPath(this, Keys.PODCAST__PODCAST_COVER_ID_FKEY, null);

        return _cover;
    }

    private transient ItemPath _item;

    /**
     * Get the implicit to-many join path to the <code>public.item</code> table
     */
    public ItemPath item() {
        if (_item == null)
            _item = new ItemPath(this, null, Keys.ITEM__ITEM_PODCAST_ID_FKEY.getInverseKey());

        return _item;
    }

    private transient PodcastTagsPath _podcastTags;

    /**
     * Get the implicit to-many join path to the
     * <code>public.podcast_tags</code> table
     */
    public PodcastTagsPath podcastTags() {
        if (_podcastTags == null)
            _podcastTags = new PodcastTagsPath(this, null, Keys.PODCAST_TAGS__PODCAST_TAGS_PODCASTS_ID_FKEY.getInverseKey());

        return _podcastTags;
    }

    /**
     * Get the implicit many-to-many join path to the <code>public.tag</code>
     * table
     */
    public TagPath tag() {
        return podcastTags().tag();
    }

    @Override
    public Podcast as(String alias) {
        return new Podcast(DSL.name(alias), this);
    }

    @Override
    public Podcast as(Name alias) {
        return new Podcast(alias, this);
    }

    @Override
    public Podcast as(Table<?> alias) {
        return new Podcast(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Podcast rename(String name) {
        return new Podcast(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Podcast rename(Name name) {
        return new Podcast(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Podcast rename(Table<?> name) {
        return new Podcast(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Podcast where(Condition condition) {
        return new Podcast(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Podcast where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Podcast where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Podcast where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Podcast where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Podcast where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Podcast where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Podcast where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Podcast whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Podcast whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
