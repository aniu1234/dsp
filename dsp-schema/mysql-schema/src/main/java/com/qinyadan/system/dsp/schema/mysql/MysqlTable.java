package com.qinyadan.system.dsp.schema.mysql;

import com.qinyadan.system.dsp.schema.common.enumerator.BasicEnumerator;
import com.qinyadan.system.dsp.schema.common.util.ResultSetUtils;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.enumerable.EnumerableTableScan;
import org.apache.calcite.adapter.java.AbstractQueryableTable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.impl.AbstractTableQueryable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MysqlTable extends AbstractQueryableTable implements TranslatableTable {

    private final String qualifiedTable;
    private final String selectSql;
    private final MysqlConnectionProvider connectionProvider;
    private volatile TableMetadata metadata;

    MysqlTable(String schema, String tableName, MysqlConnectionProvider connectionProvider) {
        super(Object[].class);
        qualifiedTable = quote(schema) + "." + quote(tableName);
        selectSql = "select * from " + qualifiedTable;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema,
                                        String tableName) {
        return new AbstractTableQueryable<T>(queryProvider, schema, this, tableName) {
            @Override
            public Enumerator<T> enumerator() {
                Iterator<Object[]> rows = new MysqlRowReader(
                        selectSql, connectionProvider).readData();
                return (Enumerator<T>) new BasicEnumerator(rows);
            }
        };
    }

    @Override
    public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable relOptTable) {
        return new EnumerableTableScan(context.getCluster(),
                context.getCluster().traitSetOf(EnumerableConvention.INSTANCE),
                relOptTable, (Class) getElementType());
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        TableMetadata tableMetadata = metadata();
        List<RelDataType> columnTypes = new ArrayList<>(tableMetadata.types.size());
        for (Class<?> columnType : tableMetadata.types) {
            columnTypes.add(typeFactory.createJavaType(columnType));
        }
        return typeFactory.createStructType(columnTypes, tableMetadata.names);
    }

    private TableMetadata metadata() {
        TableMetadata current = metadata;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (metadata == null) {
                metadata = loadMetadata();
            }
            return metadata;
        }
    }

    private TableMetadata loadMetadata() {
        String sql = selectSql + " where 1 = 0";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return new TableMetadata(ResultSetUtils.getColumnNameFromResultSet(resultSet),
                    ResultSetUtils.getColumnTypeFromResultSet(resultSet));
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load MySQL table metadata for "
                    + qualifiedTable, e);
        }
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static final class TableMetadata {
        private final List<String> names;
        private final List<Class> types;

        private TableMetadata(List<String> names, List<Class> types) {
            this.names = names;
            this.types = types;
        }
    }
}
