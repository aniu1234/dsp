package com.qinyadan.system.dsp.core.util;


import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public final class TimeUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd");

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private TimeUtils() {
    }

    public static Long getDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMATTER)
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (DateTimeParseException dateError) {
            try {
                return LocalDateTime.parse(date, TIMESTAMP_FORMATTER)
                        .toInstant(ZoneOffset.UTC).toEpochMilli();
            } catch (DateTimeParseException timestampError) {
                return null;
            }
        }
    }

    public static String formatDate(Date date) {
        // Preserve the historical null fallback; Value never calls this for SQL NULL.
        return date == null ? "1970-01-01" : Instant.ofEpochMilli(date.getTime())
                .atOffset(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER);
    }

    public static String formatTimestamp(java.util.Date date) {
        return date == null ? "1970-01-01 00:00:00" : Instant.ofEpochMilli(date.getTime())
                .atOffset(ZoneOffset.UTC).toLocalDateTime().format(TIMESTAMP_FORMATTER);
    }
}
