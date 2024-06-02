/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables;


import com.github.davinkevin.podcastserver.database.Keys;
import com.github.davinkevin.podcastserver.database.Public;
import com.github.davinkevin.podcastserver.database.tables.Item.ItemPath;
import com.github.davinkevin.podcastserver.database.tables.WatchList.WatchListPath;
import com.github.davinkevin.podcastserver.database.tables.records.WatchListItemsRecord;

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
public class WatchListItems extends TableImpl<WatchListItemsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.watch_list_items</code>
     */
    public static final WatchListItems WATCH_LIST_ITEMS = new WatchListItems();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<WatchListItemsRecord> getRecordType() {
        return WatchListItemsRecord.class;
    }

    /**
     * The column <code>public.watch_list_items.watch_lists_id</code>.
     */
    public final TableField<WatchListItemsRecord, UUID> WATCH_LISTS_ID = createField(DSL.name("watch_lists_id"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>public.watch_list_items.items_id</code>.
     */
    public final TableField<WatchListItemsRecord, UUID> ITEMS_ID = createField(DSL.name("items_id"), SQLDataType.UUID.nullable(false), this, "");

    private WatchListItems(Name alias, Table<WatchListItemsRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private WatchListItems(Name alias, Table<WatchListItemsRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.watch_list_items</code> table reference
     */
    public WatchListItems(String alias) {
        this(DSL.name(alias), WATCH_LIST_ITEMS);
    }

    /**
     * Create an aliased <code>public.watch_list_items</code> table reference
     */
    public WatchListItems(Name alias) {
        this(alias, WATCH_LIST_ITEMS);
    }

    /**
     * Create a <code>public.watch_list_items</code> table reference
     */
    public WatchListItems() {
        this(DSL.name("watch_list_items"), null);
    }

    public <O extends Record> WatchListItems(Table<O> path, ForeignKey<O, WatchListItemsRecord> childPath, InverseForeignKey<O, WatchListItemsRecord> parentPath) {
        super(path, childPath, parentPath, WATCH_LIST_ITEMS);
    }

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    public static class WatchListItemsPath extends WatchListItems implements Path<WatchListItemsRecord> {

        private static final long serialVersionUID = 1L;
        public <O extends Record> WatchListItemsPath(Table<O> path, ForeignKey<O, WatchListItemsRecord> childPath, InverseForeignKey<O, WatchListItemsRecord> parentPath) {
            super(path, childPath, parentPath);
        }
        private WatchListItemsPath(Name alias, Table<WatchListItemsRecord> aliased) {
            super(alias, aliased);
        }

        @Override
        public WatchListItemsPath as(String alias) {
            return new WatchListItemsPath(DSL.name(alias), this);
        }

        @Override
        public WatchListItemsPath as(Name alias) {
            return new WatchListItemsPath(alias, this);
        }

        @Override
        public WatchListItemsPath as(Table<?> alias) {
            return new WatchListItemsPath(alias.getQualifiedName(), this);
        }
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<WatchListItemsRecord> getPrimaryKey() {
        return Keys.WATCH_LIST_ITEMS_PKEY;
    }

    @Override
    public List<ForeignKey<WatchListItemsRecord, ?>> getReferences() {
        return Arrays.asList(Keys.WATCH_LIST_ITEMS__WATCH_LIST_ITEMS_WATCH_LISTS_ID_FKEY, Keys.WATCH_LIST_ITEMS__WATCH_LIST_ITEMS_ITEMS_ID_FKEY);
    }

    private transient WatchListPath _watchList;

    /**
     * Get the implicit join path to the <code>public.watch_list</code> table.
     */
    public WatchListPath watchList() {
        if (_watchList == null)
            _watchList = new WatchListPath(this, Keys.WATCH_LIST_ITEMS__WATCH_LIST_ITEMS_WATCH_LISTS_ID_FKEY, null);

        return _watchList;
    }

    private transient ItemPath _item;

    /**
     * Get the implicit join path to the <code>public.item</code> table.
     */
    public ItemPath item() {
        if (_item == null)
            _item = new ItemPath(this, Keys.WATCH_LIST_ITEMS__WATCH_LIST_ITEMS_ITEMS_ID_FKEY, null);

        return _item;
    }

    @Override
    public WatchListItems as(String alias) {
        return new WatchListItems(DSL.name(alias), this);
    }

    @Override
    public WatchListItems as(Name alias) {
        return new WatchListItems(alias, this);
    }

    @Override
    public WatchListItems as(Table<?> alias) {
        return new WatchListItems(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public WatchListItems rename(String name) {
        return new WatchListItems(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public WatchListItems rename(Name name) {
        return new WatchListItems(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public WatchListItems rename(Table<?> name) {
        return new WatchListItems(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public WatchListItems where(Condition condition) {
        return new WatchListItems(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public WatchListItems where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public WatchListItems where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public WatchListItems where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public WatchListItems where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public WatchListItems where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public WatchListItems where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public WatchListItems where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public WatchListItems whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public WatchListItems whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
