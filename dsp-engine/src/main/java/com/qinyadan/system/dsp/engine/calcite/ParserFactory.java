package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.engine.rules.SlothRules;
import com.qinyadan.system.dsp.engine.rules.cbo.SlothPhysicalJoinChooseRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.rules.JoinPushThroughJoinRule;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import com.qinyadan.system.calcite.sql.parser.impl.SqlSchemaParserImpl;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;

import java.util.Objects;
import java.util.Properties;

import static com.qinyadan.system.dsp.engine.rules.SlothRules.BASE_RULES;
import static com.qinyadan.system.dsp.engine.rules.SlothRules.CONSTANT_REDUCTION_RULES;


@Slf4j
public class ParserFactory {

    public static final CalciteCatalogReader CALCITE_CATALOG_READER = new CalciteCatalogReader(
            CalciteSchema.createRootSchema(false),
            ImmutableList.of(),
            new JavaTypeFactoryImpl(),
            new CalciteConnectionConfigImpl(new Properties())
    );


    public static SlothParser getParser(String sql, String currentDb) {
        CalciteCatalogReader calciteCatalogReader = getCatalogReader();

        if (Objects.nonNull(currentDb)) {
            calciteCatalogReader = calciteCatalogReader.withSchemaPath(Lists.newArrayList(currentDb));
        }
        return new SlothParser(getSqlParser(sql), getOptPlanner(),
                calciteCatalogReader, createSqlValidator(calciteCatalogReader));
    }

    public static SlothParser getParserWithCatalogReader(String sql, CalciteCatalogReader calciteCatalogReader) {
        return new SlothParser(getSqlParser(sql), getOptPlanner(),
                calciteCatalogReader, createSqlValidator(calciteCatalogReader));
    }


    public static RelOptPlanner getOptPlanner() {
        final VolcanoPlanner volcanoPlanner = new VolcanoPlanner();
        volcanoPlanner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        volcanoPlanner.setExecutor(RexUtil.EXECUTOR);

        /**
         * See {VolcanoPlanner#getCost(
         * RelNode, RelMetadataQuery)}
         */
        volcanoPlanner.setNoneConventionHasInfiniteCost(false);

        registerRules(volcanoPlanner);

        for (RelOptRule relOptRule : SlothRules.CONVERTER_RULE) {
            volcanoPlanner.addRule(relOptRule);
        }

        volcanoPlanner.addRule(SlothPhysicalJoinChooseRule.INSTANCE);


        //默认情况下plan有convertion 这个traitset,只需要加下需要加的traitset即可
        volcanoPlanner.addRelTraitDef(RelCollationTraitDef.INSTANCE);
        return volcanoPlanner;
    }

    public static void registerRules(RelOptPlanner relOptPlanner) {
        for (RelOptRule relOptRule : CONSTANT_REDUCTION_RULES) {
            relOptPlanner.addRule(relOptRule);
        }

        RelOptUtil.registerAbstractRelationalRules(relOptPlanner);
        RelOptUtil.registerAbstractRules(relOptPlanner);

        for (RelOptRule relOptRule : BASE_RULES) {
            relOptPlanner.addRule(relOptRule);
        }

        relOptPlanner.addRule(CoreRules.PROJECT_TABLE_SCAN);
        relOptPlanner.addRule(CoreRules.PROJECT_INTERPRETER_TABLE_SCAN);
        relOptPlanner.addRule(CoreRules.FILTER_REDUCE_EXPRESSIONS);

        //TODO 尝试添加适配SlothConvention JoinToMultiJoinRule, 然后在
        /**
         * {@link org.apache.calcite.rel.rules.JoinToMultiJoinRule}
         * {@link org.apache.calcite.rel.rules.MultiJoinOptimizeBushyRule}
         * {@link org.apache.calcite.rel.rules.LoptOptimizeJoinRule}
         */
        relOptPlanner.addRule(CoreRules.JOIN_COMMUTE);
        relOptPlanner.addRule(CoreRules.JOIN_ASSOCIATE);

        relOptPlanner.addRule(CoreRules.JOIN_TO_MULTI_JOIN);

        //this two we can only need one
        relOptPlanner.addRule(CoreRules.MULTI_JOIN_OPTIMIZE_BUSHY);
        relOptPlanner.addRule(CoreRules.MULTI_JOIN_OPTIMIZE);

        relOptPlanner.addRule(JoinPushThroughJoinRule.LEFT);
        relOptPlanner.addRule(JoinPushThroughJoinRule.RIGHT);

        //relOptPlanner.addRule(SlothPhysicalJoinChooseRule.INSTANCE);

        //Currently when introduce with relcollation rule, SortRemoveRule has bug
        //sort remove this rule temporarily
        relOptPlanner.removeRule(CoreRules.SORT_REMOVE);
    }


    public static SqlParser getSqlParser(String sql) {
        final SqlParser.ConfigBuilder sqlBuilder = SqlParser.configBuilder()
                .setLex(Lex.MYSQL)
                .setQuoting(Quoting.BACK_TICK)
                .setQuotedCasing(Casing.UNCHANGED)
                .setUnquotedCasing(Casing.UNCHANGED)
                .setCaseSensitive(false)
                .setConformance(SqlConformanceEnum.MYSQL_5)
                .setParserFactory(SqlSchemaParserImpl.FACTORY);


        SqlParser.Config config = sqlBuilder.build();

        return SqlParser.create(sql, config);
    }

    public static CalciteCatalogReader getCatalogReader() {
        return CALCITE_CATALOG_READER;
    }

    public static SqlValidator createSqlValidator(CalciteCatalogReader calciteCatalogReader) {
        final SqlOperatorTable operatorTable1 = calciteCatalogReader.getConfig().fun(
                SqlOperatorTable.class,
                SqlStdOperatorTable.instance());


//        final SqlOperatorTable operatorTable2 = ChainedSqlOperatorTable.of(operatorTable1, calciteCatalogReader);
        final RelDataTypeFactory factory = calciteCatalogReader.getTypeFactory();

        return SqlValidatorUtil.newValidator(operatorTable1, calciteCatalogReader, factory, SqlValidator.Config.DEFAULT);
    }

}
