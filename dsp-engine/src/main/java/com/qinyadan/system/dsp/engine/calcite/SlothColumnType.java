package com.qinyadan.system.dsp.engine.calcite;

import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlTypeNameSpec;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.Objects;


public class SlothColumnType extends SqlDataTypeSpec {

    private SqlNode precision;

    /**
     * Currently java can't implement unsigned;
     */
    private boolean unsigned;

    /**
     * default value;
     */
    private SqlNode defaultValue;

    /**
     * comment
     */
    private SqlNode comment;

    public SlothColumnType(SqlDataTypeSpec sqlDataTypeSpec, SqlNode precision, boolean unsigned,
                           SqlNode defaultValue, SqlNode comment) {
        super(sqlDataTypeSpec.getTypeNameSpec(), null, sqlDataTypeSpec.getNullable(), sqlDataTypeSpec.getParserPosition());
        this.precision = precision;
        this.unsigned = unsigned;
        this.defaultValue = defaultValue;
        this.comment = comment;
    }

    public SlothColumnType(SqlTypeNameSpec typeNameSpec, SqlParserPos pos, SqlNode precision, boolean unsigned,
                           SqlNode defaultValue, SqlNode comment) {
        super(typeNameSpec, pos);
        this.precision = precision;
        this.unsigned = unsigned;
        this.defaultValue = defaultValue;
        this.comment = comment;
    }

    public SqlNode getPrecision() {
        return precision;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public SqlNode getDefaultValue() {
        return defaultValue;
    }

    public SqlNode getComment() {
        return comment;
    }

    @Override
    public Boolean getNullable() {
        return super.getNullable();
    }

    public EnhanceSlothColumn toEnhance(String columnName) {
        EnhanceSlothColumn enhanceSlothColumn = new EnhanceSlothColumn();

        enhanceSlothColumn.setColumName(columnName);
        enhanceSlothColumn.setColumnComment(literalValue(comment));

        final String sqlTypeNameString = getTypeNameSpec().getTypeName().toString().toUpperCase();
        final SqlTypeName sqlTypeName = SqlTypeName.get(sqlTypeNameString);

        enhanceSlothColumn.setColumnType(sqlTypeName);
        enhanceSlothColumn.setUnsigned(unsigned);
        enhanceSlothColumn.setDefalutValue(literalValue(defaultValue));
        enhanceSlothColumn.setNullable(getNullable());
        if (Objects.nonNull(precision)) {
            enhanceSlothColumn.setPrecision(Integer.parseInt(precision.toString()));
        }

        return enhanceSlothColumn;
    }

    private String literalValue(SqlNode node) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlCharStringLiteral) {
            return ((SqlCharStringLiteral) node).getNlsString().getValue();
        }
        if (node instanceof SqlLiteral) {
            Object value = ((SqlLiteral) node).getValue();
            return value == null ? null : value.toString();
        }
        return node.toString();
    }
}
