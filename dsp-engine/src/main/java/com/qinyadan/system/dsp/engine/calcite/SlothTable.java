package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.engine.constant.FileConstants;
import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.adapter.java.AbstractQueryableTable;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.List;
import java.util.Objects;
import java.nio.file.Paths;


public class SlothTable extends AbstractQueryableTable {

    public static final String DEFAULT_ENGINE_NAME = "lucene";

    public static final int DEFAULT_SHARD = 1;

    @Getter
    @Setter
    private String tableName;

    @Getter
    @Setter
    private SlothSchema schema;

    @Getter
    @Setter
    private List<SlothColumn> columns;

    private RelDataType resultType;

    /**
     * Engine name, currently DEFAULT_ENGINE_NAME
     */
    @Getter
    @Setter
    private String engineName = DEFAULT_ENGINE_NAME;

    /**
     * Table comment
     */
    @Getter
    @Setter
    private String tableComment;

    /**
     * Shard number
     */
    @Getter
    @Setter
    private int shardNum = DEFAULT_SHARD;

    @Getter
    private SlothTableEngine slothTableEngine;

    public SlothTable(String tableName, SlothSchema schema, RelDataType resultType) {
        super(Object[].class);
        this.tableName = tableName;
        this.schema = schema;
        this.resultType = resultType;
    }


    public SlothTable() {
        super(Object[].class);
    }

    public SlothTable(String tableName) {
        this();
        this.tableName = tableName;
    }

    public SlothTable(SlothSchema slothSchema) {
        super(Object[].class);
        this.schema = slothSchema;
    }

    public String buildTableEnginePath() {
        return Paths.get(FileConstants.getTableFileLocation(), schema.getSchemaName(), tableName).toString();
    }


    void initTableEngine() {
        slothTableEngine = new SlothTableEngine(this);
    }

    @Override
    public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema, String tableName) {
        return null;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {

        if (Objects.nonNull(resultType)) {
            return resultType;
        }

        List<RelDataType> relDataTypes = Lists.newArrayListWithCapacity(columns.size());
        List<String> columnNames = Lists.newArrayList();

        //date/time/datetime 都用long存储
        columns.forEach(cl -> {
            final SqlTypeName sqlTypeName = cl.getColumnType().getColumnType();
            RelDataType relDataType = typeFactory.createSqlType(sqlTypeName);
            relDataType = typeFactory.createTypeWithNullability(
                    relDataType, cl.getColumnType().isNullable());

            relDataTypes.add(relDataType);
            columnNames.add(cl.getColumnName());
        });

        resultType = typeFactory.createStructType(relDataTypes, columnNames);
        return resultType;
    }

    /**
     * You cant set table staticstic here
     * such as RelCollation and statatisc will be use in
     * org.apache.calcite.rel.logical.LogicalTableScan#create(RelOptCluster, RelOptTable, List)}
     *
     * @return
     */
    @Override
    public Statistic getStatistic() {
        return super.getStatistic();
    }

    //TODO 高优, 这里需要找一下原因， 没有 SlothTableScanConverterRule RULE的话，直接会报错
    // 直接toRel有问题， 在直接select * from table的情况下
//    @Override
//    public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable relOptTable) {
//        return new SlothTableScan(
//                context.getCluster(),
//                RelTraitSet.createEmpty()
//                        .plus(RelCollations.EMPTY)
//                        .plus(SlothConvention.INSTANCE),
//                relOptTable);
//    }
}
