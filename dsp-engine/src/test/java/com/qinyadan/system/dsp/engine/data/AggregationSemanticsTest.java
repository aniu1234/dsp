package com.qinyadan.system.dsp.engine.data;

import com.qinyadan.system.dsp.engine.data.func.agg.CountAggregation;
import com.qinyadan.system.dsp.engine.data.func.agg.MaxAggregation;
import com.qinyadan.system.dsp.engine.data.func.agg.MinAggregation;
import com.qinyadan.system.dsp.engine.data.func.agg.SumAggregation;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.engine.data.value.Value;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class AggregationSemanticsTest {

    private final List<SlothRow> rows = Arrays.asList(
            row(1), row(null), row(3));

    @Test
    public void countColumnAlwaysIgnoresNull() {
        CountAggregation aggregation = new CountAggregation(false, false,
                DataTypes.INTEGER, DataTypes.LONG, 0, false, Collections.emptyList());
        aggregation.setOriginDatas(rows);

        assertEquals(Long.valueOf(2), aggregation.compute().longValue());
    }

    @Test
    public void sumMinAndMaxIgnoreNull() {
        SumAggregation sum = new SumAggregation(false, false, DataTypes.INTEGER,
                DataTypes.LONG, 0, Collections.emptyList());
        MinAggregation min = new MinAggregation(false, false, DataTypes.INTEGER,
                DataTypes.INTEGER, 0, Collections.emptyList());
        MaxAggregation max = new MaxAggregation(false, false, DataTypes.INTEGER,
                DataTypes.INTEGER, 0, Collections.emptyList());
        sum.setOriginDatas(rows);
        min.setOriginDatas(rows);
        max.setOriginDatas(rows);

        assertEquals(Long.valueOf(4), sum.compute().longValue());
        assertEquals(Integer.valueOf(1), min.compute().intValue());
        assertEquals(Integer.valueOf(3), max.compute().intValue());
    }

    private static SlothRow row(Integer value) {
        return new SlothRow(Collections.singletonList(new Value(value, DataTypes.INTEGER)));
    }
}
