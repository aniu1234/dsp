package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import org.apache.lucene.document.*;

import java.util.List;
import java.util.Map;

import static com.qinyadan.system.dsp.core.data.type.DataType.Precedence.*;

/**
 * Converts between {@link Row} objects and Lucene {@link Document}s.
 * Handles NULL value mapping per data type.
 */
public final class LuceneDocumentMapper {

    private final List<ColumnInfo> columns;
    private final Map<String, DataType<?>> typeMap;

    public LuceneDocumentMapper(List<ColumnInfo> columns, Map<String, DataType<?>> typeMap) {
        this.columns = columns;
        this.typeMap = typeMap;
    }

    public Document toRow(Row row) {
        Document document = new Document();
        Value[] values = row.getColumns();

        for (int i = 0; i < values.length; i++) {
            Value value = values[i];
            ColumnInfo col = columns.get(i);
            String name = col.getName();
            DataType<?> type = col.getType();

            if (value.isNull()) {
                appendNullField(document, name, type);
            } else {
                appendNonNullField(document, name, type, value);
            }
        }
        return document;
    }

    private void appendNullField(Document doc, String name, DataType<?> type) {
        switch (type.precedence()) {
            case BYTE:
            case SHORT:
            case INTEGER:
                doc.add(new IntPoint(name, Integer.MIN_VALUE));
                doc.add(new StoredField(name, Integer.MIN_VALUE));
                break;
            case LONG:
                doc.add(new LongPoint(name, Long.MIN_VALUE));
                doc.add(new StoredField(name, Long.MIN_VALUE));
                break;
            case FLOAT:
                doc.add(new FloatPoint(name, Float.MIN_VALUE));
                doc.add(new StoredField(name, Float.MIN_VALUE));
                break;
            case DOUBLE:
                doc.add(new DoublePoint(name, Double.MIN_VALUE));
                doc.add(new StoredField(name, Double.MIN_VALUE));
                break;
            case DATE:
                doc.add(new LongPoint(name, Long.MIN_VALUE));
                doc.add(new StoredField(name, Long.MIN_VALUE));
                break;
            default:
                doc.add(new StringField(name, "NULL", Field.Store.YES));
                break;
        }
    }

    private void appendNonNullField(Document doc, String name, DataType<?> type, Value value) {
        switch (type.precedence()) {
            case BYTE:
            case SHORT:
            case INTEGER:
                int intContent = ((Number) value.getValue()).intValue();
                doc.add(new IntPoint(name, intContent));
                doc.add(new StoredField(name, intContent));
                break;
            case LONG:
                long longContent = ((Number) value.getValue()).longValue();
                doc.add(new LongPoint(name, longContent));
                doc.add(new StoredField(name, longContent));
                break;
            case FLOAT:
                float floatContent = ((Number) value.getValue()).floatValue();
                doc.add(new FloatPoint(name, floatContent));
                doc.add(new StoredField(name, floatContent));
                break;
            case DOUBLE:
                double doubleContent = ((Number) value.getValue()).doubleValue();
                doc.add(new DoublePoint(name, doubleContent));
                doc.add(new StoredField(name, doubleContent));
                break;
            case DATE:
                long dateContent = ((Number) value.getValue()).longValue();
                doc.add(new LongPoint(name, dateContent));
                doc.add(new StoredField(name, dateContent));
                break;
            default:
                String strContent = value.getValue() instanceof String
                        ? (String) value.getValue()
                        : value.getValue().toString();
                doc.add(new StringField(name, strContent, Field.Store.YES));
                break;
        }
    }
}
