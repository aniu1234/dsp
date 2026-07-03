package com.qinyadan.system.dsp.storage.meta;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.storage.meta.tables.pojos.Columns;
import com.qinyadan.system.dsp.storage.meta.tables.pojos.Tables;
import com.qinyadan.system.dsp.storage.parser.EnhanceSlothColumn;
import com.qinyadan.system.dsp.storage.parser.SlothColumn;
import com.qinyadan.system.dsp.storage.parser.SlothSchema;
import com.qinyadan.system.dsp.storage.parser.SlothTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep10;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;



public class TableMeta {

    public static final TableMeta INSTANCE = new TableMeta(MysqlConnection.INSTANCE);
    private MysqlConnection mysqlConnection;

    public TableMeta(MysqlConnection mysqlConnection) {
        this.mysqlConnection = mysqlConnection;
    }

    public void addTable(String schema, SlothTable table) {
        if (!mysqlConnection.isOk()) {
            return;
        }

        final String tableName = table.getTableName();
        //add table
        final DSLContext dslContext = mysqlConnection.getDslContext();
        dslContext.insertInto(Sloth.SLOTH.TABLES)
                .columns(Sloth.SLOTH.TABLES.TABLE_CATALOG,
                        Sloth.SLOTH.TABLES.TABLE_SCHEMA,
                        Sloth.SLOTH.TABLES.TABLE_NAME,
                        Sloth.SLOTH.TABLES.TABLE_COMMENT,
                        Sloth.SLOTH.TABLES.ENGINE, Sloth.SLOTH.TABLES.TABLE_SHARD)
                .values("def",
                        schema,
                        tableName,
                        Objects.isNull(table.getTableComment()) ? "" : table.getTableComment(),
                        Objects.isNull(table.getEngineName()) ? null : table.getEngineName(),
                        table.getShardNum())
                .execute();

        //add column
        InsertValuesStep10 insertValues = dslContext.insertInto(Sloth.SLOTH.COLUMNS)
                .columns(Sloth.SLOTH.COLUMNS.TABLE_CATALOG,
                        Sloth.SLOTH.COLUMNS.TABLE_SCHEMA,
                        Sloth.SLOTH.COLUMNS.TABLE_NAME,
                        Sloth.SLOTH.COLUMNS.COLUMN_NAME,
                        Sloth.SLOTH.COLUMNS.COLUMN_TYPE,
                        Sloth.SLOTH.COLUMNS.GENERATION_EXPRESSION,
                        Sloth.SLOTH.COLUMNS.ORDINAL_POSITION,
                        Sloth.SLOTH.COLUMNS.COLUMN_DEFAULT,
                        Sloth.SLOTH.COLUMNS.IS_NULLABLE,
                        Sloth.SLOTH.COLUMNS.COLUMN_COMMENT
                );

        final List<SlothColumn> columnList = table.getColumns();
        final int size = columnList.size();

        for (int i = 0; i < size; i++) {
            insertValues = insertValues.values(
                    "def",
                    schema,
                    tableName,
                    columnList.get(i).getColumnName(),
                    columnList.get(i).getColumnType().getColumnType().getName(),
                    "",
                    i,
                    columnList.get(i).getColumnType().getDefalutValue(),
                    columnList.get(i).getColumnType().isNullable() ? "YES" : "NO",
                    Objects.isNull(columnList.get(i).getColumnType().getColumnComment()) ? "" : columnList.get(i).getColumnType().getColumnComment()
            );
        }

        insertValues.execute();
    }


    public void deleteTable(String schema, String table) {
        if (!mysqlConnection.isOk()) {
            return;
        }

        final DSLContext dslContext = mysqlConnection.getDslContext();

        //delete from tables;
        dslContext.deleteFrom(Sloth.SLOTH.TABLES)
                .where(Sloth.SLOTH.TABLES.TABLE_SCHEMA.eq(schema).and(Sloth.SLOTH.TABLES.TABLE_NAME.eq(table)))
                .execute();

        //delete from columns
        dslContext.deleteFrom(Sloth.SLOTH.COLUMNS)
                .where(Sloth.SLOTH.COLUMNS.TABLE_NAME.eq(table).and(Sloth.SLOTH.COLUMNS.TABLE_NAME.eq(table)))
                .execute();
    }

    public List<SlothTable> getAllTableInDb(SlothSchema schema) {
        if (!mysqlConnection.isOk()) {
            return Lists.newArrayList();
        }

        final String schemaName = schema.getSchemaName();
        final DSLContext dslContext = mysqlConnection.getDslContext();
        final List<Columns> columns = dslContext.selectFrom(Sloth.SLOTH.COLUMNS)
                .where(Sloth.SLOTH.COLUMNS.TABLE_SCHEMA.eq(schemaName))
                .fetchInto(Columns.class);


        List<SlothTable> result = Lists.newArrayList();
        columns.stream().collect(Collectors.groupingBy(Columns::getTableName)).forEach((tableName, cols) -> {
            final List<SlothColumn> slothColumns = cols.stream().map(c -> {
                EnhanceSlothColumn enhanceSlothColumn = new EnhanceSlothColumn();
                enhanceSlothColumn.setNullable(!Objects.equals("NO", c.getIsNullable()));
                enhanceSlothColumn.setColumName(c.getColumnName());
                enhanceSlothColumn.setColumnComment(c.getColumnComment());
                enhanceSlothColumn.setColumnType(SqlTypeName.valueOf(c.getColumnType()));
                enhanceSlothColumn.setDefalutValue(c.getColumnDefault());
                return new SlothColumn(c.getColumnName(), enhanceSlothColumn);
            }).collect(Collectors.toList());

            SlothTable slothTable = new SlothTable(tableName);

            final List<Tables> tables =
                    dslContext.selectFrom(Sloth.SLOTH.TABLES)
                            .where(Sloth.SLOTH.TABLES.TABLE_NAME.eq(tableName).and(Sloth.SLOTH.TABLES.TABLE_SCHEMA.eq(schemaName)))
                            .fetchInto(Tables.class);

            if (tables.size() != 1) {
                return;
            }
            final Tables t = tables.get(0);

            slothTable.setColumns(slothColumns);
            slothTable.setSchema(schema);
            slothTable.setShardNum(t.getTableShard());
            slothTable.setEngineName(t.getEngine());
            slothTable.setTableComment(t.getTableComment());
            slothTable.initTableEngine();
            result.add(slothTable);
        });

        return result;
    }
}
