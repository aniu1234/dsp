package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.RowImpl;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Maps storage API rows to Lucene documents without exposing Lucene details to callers.
 */
public final class LuceneDocumentMapper {

    private static final String NULL_MARKER_PREFIX = Character.toString((char) 0)
            + "dsp_null" + Character.toString((char) 0);
    private static final String LEGACY_NULL_MARKER_SUFFIX = "__dsp_null";

    private final List<ColumnInfo> columns;

    public LuceneDocumentMapper(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public Document toDocument(Row row) {
        if (row.columnSize() != columns.size()) {
            throw new IllegalArgumentException("Expected " + columns.size()
                    + " columns but got " + row.columnSize());
        }

        Document document = new Document();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo column = columns.get(i);
            Value value = row.getColumn(i);
            String name = column.getName();
            DataType<?> type = column.getType();

            document.add(new StoredField(nullMarker(name), value == null || value.isNull() ? 1 : 0));
            if (value == null || value.isNull()) {
                appendNullField(document, name, type);
            } else {
                appendValueField(document, name, type, value);
            }
        }
        return document;
    }

    public Row fromDocument(Document document, Set<String> requestedColumns) {
        List<Value> values = new ArrayList<>();
        for (ColumnInfo column : columns) {
            if (requestedColumns != null && !requestedColumns.isEmpty()
                    && !requestedColumns.contains(column.getName())) {
                continue;
            }

            IndexableField field = document.getField(column.getName());
            if (field == null) {
                values.add(new Value(null, column.getType()));
                continue;
            }

            IndexableField marker = document.getField(nullMarker(column.getName()));
            if (marker == null) {
                marker = document.getField(legacyNullMarker(column.getName()));
            }
            boolean legacyEncoding = marker == null;
            if (!legacyEncoding && marker.numericValue().intValue() == 1) {
                values.add(new Value(null, column.getType()));
                continue;
            }
            values.add(fieldToValue(field, column.getType(), legacyEncoding));
        }
        return RowImpl.of(values.toArray(new Value[0]));
    }

    static String nullMarker(String columnName) {
        return NULL_MARKER_PREFIX + columnName;
    }

    static String legacyNullMarker(String columnName) {
        return columnName + LEGACY_NULL_MARKER_SUFFIX;
    }

    private static void appendNullField(Document document, String name, DataType<?> type) {
        switch (type.precedence()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case BOOLEAN:
                addInteger(document, name, Integer.MIN_VALUE);
                break;
            case LONG:
            case DATE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIME_ZONE:
                addLong(document, name, Long.MIN_VALUE);
                break;
            case FLOAT:
                addFloat(document, name, Float.MIN_VALUE);
                break;
            case DOUBLE:
                addDouble(document, name, Double.MIN_VALUE);
                break;
            default:
                document.add(new StringField(name, "NULL", Field.Store.YES));
                break;
        }
    }

    private static void appendValueField(Document document, String name,
                                         DataType<?> type, Value value) {
        switch (type.precedence()) {
            case BYTE:
            case SHORT:
            case INTEGER:
                addInteger(document, name, ((Number) value.getValue()).intValue());
                break;
            case BOOLEAN:
                addInteger(document, name, Boolean.TRUE.equals(value.getValue()) ? 1 : 0);
                break;
            case LONG:
                addLong(document, name, ((Number) value.getValue()).longValue());
                break;
            case DATE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIME_ZONE:
                addLong(document, name, temporalValue(value));
                break;
            case FLOAT:
                addFloat(document, name, ((Number) value.getValue()).floatValue());
                break;
            case DOUBLE:
                addDouble(document, name, ((Number) value.getValue()).doubleValue());
                break;
            default:
                document.add(new StringField(name, value.stringValue(), Field.Store.YES));
                break;
        }
    }

    private static Value fieldToValue(IndexableField field, DataType<?> type,
                                      boolean legacyEncoding) {
        Number numeric = field.numericValue();
        switch (type.precedence()) {
            case BYTE:
            case SHORT:
            case INTEGER:
                return new Value(legacyEncoding && numeric.intValue() == Integer.MIN_VALUE
                        ? null : numeric.intValue(), type);
            case BOOLEAN:
                if (numeric != null) {
                    return new Value(legacyEncoding && numeric.intValue() == Integer.MIN_VALUE
                            ? null : numeric.intValue() != 0, type);
                }
                return new Value(legacyEncoding && "NULL".equals(field.stringValue())
                        ? null : Boolean.valueOf(field.stringValue()), type);
            case LONG:
            case DATE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIME_ZONE:
                return new Value(legacyEncoding && numeric.longValue() == Long.MIN_VALUE
                        ? null : numeric.longValue(), type);
            case FLOAT:
                return new Value(legacyEncoding && numeric.floatValue() == Float.MIN_VALUE
                        ? null : numeric.floatValue(), type);
            case DOUBLE:
                return new Value(legacyEncoding && numeric.doubleValue() == Double.MIN_VALUE
                        ? null : numeric.doubleValue(), type);
            default:
                return new Value(legacyEncoding && "NULL".equals(field.stringValue())
                        ? null : field.stringValue(), type);
        }
    }

    private static long temporalValue(Value value) {
        Object raw = value.getValue();
        if (raw instanceof java.util.Date) {
            return ((java.util.Date) raw).getTime();
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        Date date = value.dateValue();
        if (date == null) {
            throw new IllegalArgumentException("Unsupported temporal value: " + raw);
        }
        return date.getTime();
    }

    private static void addInteger(Document document, String name, int value) {
        document.add(new IntPoint(name, value));
        document.add(new StoredField(name, value));
    }

    private static void addLong(Document document, String name, long value) {
        document.add(new LongPoint(name, value));
        document.add(new StoredField(name, value));
    }

    private static void addFloat(Document document, String name, float value) {
        document.add(new FloatPoint(name, value));
        document.add(new StoredField(name, value));
    }

    private static void addDouble(Document document, String name, double value) {
        document.add(new DoublePoint(name, value));
        document.add(new StoredField(name, value));
    }
}
