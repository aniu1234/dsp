package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.operator.Operator;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QueryExecutionTest {

    @Test
    public void exposesProtocolNeutralRowsAndClosesOperator() {
        StubOperator operator = new StubOperator(false);
        QueryExecution execution = new QueryExecution(
                Collections.singletonList(new QueryColumn("value", Types.INTEGER)),
                operator);

        execution.open();
        assertEquals(Collections.<Object>singletonList(7), execution.nextRow());
        assertNull(execution.nextRow());
        execution.close();

        assertTrue(operator.closed);
    }

    @Test
    public void closesOperatorAfterFailedOpen() {
        StubOperator operator = new StubOperator(true);
        QueryExecution execution = new QueryExecution(Collections.<QueryColumn>emptyList(),
                operator);

        try {
            execution.open();
        } catch (IllegalStateException expected) {
            execution.close();
        }

        assertTrue(operator.closed);
    }

    private static final class StubOperator implements Operator<SlothRow> {

        private final boolean failOpen;
        private int index;
        private boolean closed;

        private StubOperator(boolean failOpen) {
            this.failOpen = failOpen;
        }

        @Override
        public void open() {
            if (failOpen) {
                throw new IllegalStateException("open failed");
            }
        }

        @Override
        public SlothRow next() {
            if (index++ == 0) {
                return new SlothRow(Collections.singletonList(
                        new Value(7, DataTypes.INTEGER)));
            }
            return SlothRow.EOF_ROW;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public List<DataType> getRowType() {
            return Arrays.<DataType>asList(DataTypes.INTEGER);
        }
    }
}
