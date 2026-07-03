package com.qinyadan.system.dsp.engine.calcite;

import lombok.Data;
import org.apache.calcite.sql.type.SqlTypeName;


@Data
public class EnhanceSlothColumn {

    /**
     * Column name
     */
    private String columName;

    /**
     * Column type
     */
    private SqlTypeName columnType;

    /**
     * True is unsigned, false not
     * <p>
     * Attention, Currently java cant implement unsigned syntax
     */
    private boolean unsigned;

    /**
     * Whether the column can be null
     */
    private boolean nullable;

    /**
     * All kind of default value set string
     */
    private String defalutValue;

    /**
     * String column comment
     */
    private String columnComment;

    /**
     * Not used now
     */
    private int precision;

}
