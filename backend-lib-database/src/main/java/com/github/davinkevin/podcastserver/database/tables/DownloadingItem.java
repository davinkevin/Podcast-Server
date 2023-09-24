/*
 * This file is generated by jOOQ.
 */
package com.github.davinkevin.podcastserver.database.tables;


import com.github.davinkevin.podcastserver.database.Keys;
import com.github.davinkevin.podcastserver.database.Public;
import com.github.davinkevin.podcastserver.database.enums.DownloadingState;
import com.github.davinkevin.podcastserver.database.tables.records.DownloadingItemRecord;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.SelectField;
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
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DownloadingItem extends TableImpl<DownloadingItemRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.downloading_item</code>
     */
    public static final DownloadingItem DOWNLOADING_ITEM = new DownloadingItem();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DownloadingItemRecord> getRecordType() {
        return DownloadingItemRecord.class;
    }

    /**
     * The column <code>public.downloading_item.item_id</code>.
     */
    public final TableField<DownloadingItemRecord, UUID> ITEM_ID = createField(DSL.name("item_id"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>public.downloading_item.position</code>.
     */
    public final TableField<DownloadingItemRecord, Integer> POSITION = createField(DSL.name("position"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.downloading_item.state</code>.
     */
    public final TableField<DownloadingItemRecord, DownloadingState> STATE = createField(DSL.name("state"), SQLDataType.VARCHAR.nullable(false).defaultValue(DSL.field(DSL.raw("'WAITING'::downloading_state"), SQLDataType.VARCHAR)).asEnumDataType(com.github.davinkevin.podcastserver.database.enums.DownloadingState.class), this, "");

    private DownloadingItem(Name alias, Table<DownloadingItemRecord> aliased) {
        this(alias, aliased, null);
    }

    private DownloadingItem(Name alias, Table<DownloadingItemRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.downloading_item</code> table reference
     */
    public DownloadingItem(String alias) {
        this(DSL.name(alias), DOWNLOADING_ITEM);
    }

    /**
     * Create an aliased <code>public.downloading_item</code> table reference
     */
    public DownloadingItem(Name alias) {
        this(alias, DOWNLOADING_ITEM);
    }

    /**
     * Create a <code>public.downloading_item</code> table reference
     */
    public DownloadingItem() {
        this(DSL.name("downloading_item"), null);
    }

    public <O extends Record> DownloadingItem(Table<O> child, ForeignKey<O, DownloadingItemRecord> key) {
        super(child, key, DOWNLOADING_ITEM);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<DownloadingItemRecord> getPrimaryKey() {
        return Keys.DOWNLOADING_ITEM_PKEY;
    }

    @Override
    public List<UniqueKey<DownloadingItemRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.DOWNLOADING_ITEM_POSITION_KEY);
    }

    @Override
    public List<ForeignKey<DownloadingItemRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DOWNLOADING_ITEM__DOWNLOADING_ITEM_ITEM_ID_FKEY);
    }

    private transient Item _item;

    /**
     * Get the implicit join path to the <code>public.item</code> table.
     */
    public Item item() {
        if (_item == null)
            _item = new Item(this, Keys.DOWNLOADING_ITEM__DOWNLOADING_ITEM_ITEM_ID_FKEY);

        return _item;
    }

    @Override
    public DownloadingItem as(String alias) {
        return new DownloadingItem(DSL.name(alias), this);
    }

    @Override
    public DownloadingItem as(Name alias) {
        return new DownloadingItem(alias, this);
    }

    @Override
    public DownloadingItem as(Table<?> alias) {
        return new DownloadingItem(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DownloadingItem rename(String name) {
        return new DownloadingItem(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DownloadingItem rename(Name name) {
        return new DownloadingItem(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DownloadingItem rename(Table<?> name) {
        return new DownloadingItem(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<UUID, Integer, DownloadingState> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super UUID, ? super Integer, ? super DownloadingState, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super UUID, ? super Integer, ? super DownloadingState, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}