package com.qinyadan.system.dsp.engine.service.dto;

import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.operator.Operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Executable query and immutable result metadata returned by QueryService.
 */
public final class QueryExecution {

    private final Operator<SlothRow> operator;
    private final List<QueryColumn> columns;

    public QueryExecution(Operator<SlothRow> operator, List<QueryColumn> columns) {
        if (operator == null || columns == null) {
            throw new IllegalArgumentException("Query operator and columns are required");
        }
        this.operator = operator;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
    }

    public Operator<SlothRow> getOperator() {
        return operator;
    }

    public List<QueryColumn> getColumns() {
        return columns;
    }
}
