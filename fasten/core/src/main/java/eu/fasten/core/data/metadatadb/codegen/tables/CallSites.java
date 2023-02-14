/*
 * This file is generated by jOOQ.
 */
package eu.fasten.core.data.metadatadb.codegen.tables;


import eu.fasten.core.data.metadatadb.codegen.Keys;
import eu.fasten.core.data.metadatadb.codegen.Public;
import eu.fasten.core.data.metadatadb.codegen.enums.CallType;
import eu.fasten.core.data.metadatadb.codegen.tables.records.CallSitesRecord;

import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
import org.jooq.Schema;
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
@Generated(
    value = {
        "https://www.jooq.org",
        "jOOQ version:3.16.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CallSites extends TableImpl<CallSitesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.call_sites</code>
     */
    public static final CallSites CALL_SITES = new CallSites();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<CallSitesRecord> getRecordType() {
        return CallSitesRecord.class;
    }

    /**
     * The column <code>public.call_sites.source_id</code>.
     */
    public final TableField<CallSitesRecord, Long> SOURCE_ID = createField(DSL.name("source_id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.call_sites.target_id</code>.
     */
    public final TableField<CallSitesRecord, Long> TARGET_ID = createField(DSL.name("target_id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.call_sites.line</code>.
     */
    public final TableField<CallSitesRecord, Integer> LINE = createField(DSL.name("line"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>public.call_sites.call_type</code>.
     */
    public final TableField<CallSitesRecord, CallType> CALL_TYPE = createField(DSL.name("call_type"), SQLDataType.VARCHAR.asEnumDataType(eu.fasten.core.data.metadatadb.codegen.enums.CallType.class), this, "");

    /**
     * The column <code>public.call_sites.receiver_type_ids</code>.
     */
    public final TableField<CallSitesRecord, Long[]> RECEIVER_TYPE_IDS = createField(DSL.name("receiver_type_ids"), SQLDataType.BIGINT.getArrayDataType(), this, "");

    /**
     * The column <code>public.call_sites.metadata</code>.
     */
    public final TableField<CallSitesRecord, JSONB> METADATA = createField(DSL.name("metadata"), SQLDataType.JSONB, this, "");

    private CallSites(Name alias, Table<CallSitesRecord> aliased) {
        this(alias, aliased, null);
    }

    private CallSites(Name alias, Table<CallSitesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.call_sites</code> table reference
     */
    public CallSites(String alias) {
        this(DSL.name(alias), CALL_SITES);
    }

    /**
     * Create an aliased <code>public.call_sites</code> table reference
     */
    public CallSites(Name alias) {
        this(alias, CALL_SITES);
    }

    /**
     * Create a <code>public.call_sites</code> table reference
     */
    public CallSites() {
        this(DSL.name("call_sites"), null);
    }

    public <O extends Record> CallSites(Table<O> child, ForeignKey<O, CallSitesRecord> key) {
        super(child, key, CALL_SITES);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public List<UniqueKey<CallSitesRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.UNIQUE_SOURCE_TARGET);
    }

    @Override
    public List<ForeignKey<CallSitesRecord, ?>> getReferences() {
        return Arrays.asList(Keys.CALL_SITES__CALL_SITES_SOURCE_ID_FKEY, Keys.CALL_SITES__CALL_SITES_TARGET_ID_FKEY);
    }

    private transient Callables _callSitesSourceIdFkey;
    private transient Callables _callSitesTargetIdFkey;

    /**
     * Get the implicit join path to the <code>public.callables</code> table,
     * via the <code>call_sites_source_id_fkey</code> key.
     */
    public Callables callSitesSourceIdFkey() {
        if (_callSitesSourceIdFkey == null)
            _callSitesSourceIdFkey = new Callables(this, Keys.CALL_SITES__CALL_SITES_SOURCE_ID_FKEY);

        return _callSitesSourceIdFkey;
    }

    /**
     * Get the implicit join path to the <code>public.callables</code> table,
     * via the <code>call_sites_target_id_fkey</code> key.
     */
    public Callables callSitesTargetIdFkey() {
        if (_callSitesTargetIdFkey == null)
            _callSitesTargetIdFkey = new Callables(this, Keys.CALL_SITES__CALL_SITES_TARGET_ID_FKEY);

        return _callSitesTargetIdFkey;
    }

    @Override
    public CallSites as(String alias) {
        return new CallSites(DSL.name(alias), this);
    }

    @Override
    public CallSites as(Name alias) {
        return new CallSites(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public CallSites rename(String name) {
        return new CallSites(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public CallSites rename(Name name) {
        return new CallSites(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Long, Long, Integer, CallType, Long[], JSONB> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}
