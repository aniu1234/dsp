package com.qinyadan.system.dsp.storage.parser.rel.operator;

import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.runtime.engine.data.expr.Symbol;
import org.apache.calcite.rel.type.RelDataType;

import java.util.List;
import java.util.stream.Collectors;


public class SlothProjectOperator extends AbstractOperator<SlothRow> {
    private Operator<SlothRow> child;
    private List<Symbol> projects;

    public SlothProjectOperator(Operator<SlothRow> child, List<Symbol> projects, RelDataType rowType) {
        super(rowType);
        this.child = child;
        this.projects = projects;
    }

    @Override
    public void open() {
        child.open();

        //todo your one open logical
    }

    @Override
    public SlothRow next() {
        final SlothRow input = child.next();
        if (input == SlothRow.EOF_ROW) {
            return SlothRow.EOF_ROW;
        }

        projects.forEach(symbol -> symbol.setInput(input.getAllColumn()));
        return new SlothRow(projects.stream().map(Symbol::compute).collect(Collectors.toList()));
    }

    @Override
    public void close() {
        child.close();
    }


}
