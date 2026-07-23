package com.qinyadan.system.dsp.engine.meta;

import com.google.common.collect.Sets;
import com.qinyadan.system.dsp.engine.meta.tables.pojos.Schemata;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SchemaMeta {


    public static final SchemaMeta INSTANCE = new SchemaMeta(MysqlConnection.INSTANCE);

    private MysqlConnection mysqlConnection;

    public SchemaMeta(MysqlConnection mysqlConnection) {
        this.mysqlConnection = mysqlConnection;
    }

    public boolean schemaIsOk() {
        return mysqlConnection.isOk();
    }

    public Set<String> allSchema() {
        if (!mysqlConnection.isOk()) {
            return LocalMetadataStore.INSTANCE.allSchemas();
        }

        final DSLContext dslContext = mysqlConnection.getDslContext();
        List<Schemata> schemataList = dslContext.selectFrom(Sloth.SLOTH.SCHEMATA)
                .fetchInto(Schemata.class);

        return schemataList.stream().map(Schemata::getSchemaName).collect(Collectors.toSet());
    }


    public void dropSchema(String schema) {
        if (!mysqlConnection.isOk()) {
            LocalMetadataStore.INSTANCE.dropSchema(schema);
            return;
        }

        final DSLContext dslContext = mysqlConnection.getDslContext();
        dslContext.transaction(configuration -> {
            DSLContext transaction = DSL.using(configuration);
            transaction.deleteFrom(Sloth.SLOTH.COLUMNS)
                    .where(Sloth.SLOTH.COLUMNS.TABLE_SCHEMA.eq(schema)).execute();
            transaction.deleteFrom(Sloth.SLOTH.TABLES)
                    .where(Sloth.SLOTH.TABLES.TABLE_SCHEMA.eq(schema)).execute();
            final int result = transaction.deleteFrom(Sloth.SLOTH.SCHEMATA)
                    .where(Sloth.SLOTH.SCHEMATA.SCHEMA_NAME.eq(schema)).execute();
            if (result == 0) {
                log.warn("schema '{}' does not exist, pay attention...", schema);
            }
        });
    }

    public void addSchema(String schema) {
        if (!mysqlConnection.isOk()) {
            LocalMetadataStore.INSTANCE.addSchema(schema);
            return;
        }

        //TODO remove dsl and use mybatis, or you can use spring to implement this
        final DSLContext dslContext = mysqlConnection.getDslContext();
        final int count = dslContext.selectCount().from(Sloth.SLOTH.SCHEMATA)
                .where(Sloth.SLOTH.SCHEMATA.SCHEMA_NAME.eq(schema))
                .fetchOne()
                .value1();


        if (count > 0) {
            log.warn("schema '{}' already exist, omit .... ", schema);
            return;
        }

        dslContext.insertInto(Sloth.SLOTH.SCHEMATA)
                .values("def", schema, "utf8", "utf8_general_ci", null)
                .execute();
    }
}
