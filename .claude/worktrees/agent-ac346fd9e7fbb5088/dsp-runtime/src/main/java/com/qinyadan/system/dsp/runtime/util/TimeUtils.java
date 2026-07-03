package com.qinyadan.system.dsp.runtime.util;


import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;


@Slf4j
public class TimeUtils {

    public static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd");

    public static final SimpleDateFormat TIMESTAMP_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Long getDate(String date) {
        try {
            return DATE_FORMATTER.parse(date).getTime();
        } catch (ParseException e1) {
            log.error("Parser date '{}' using format '{}' error, try to use format '{}'",
                    date,
                    DATE_FORMATTER.toPattern(),
                    TIMESTAMP_FORMATTER.toPattern()
            );
            try {
                return TIMESTAMP_FORMATTER.parse(date).getTime();
            } catch (ParseException e2) {
                log.error("Parser date '{}' using format '{}' error, return null",
                        date,
                        TIMESTAMP_FORMATTER.toPattern());
                return null;
            }
        }
    }

    public static String formatDate(Date date) {
        //treat null as "1970-01-01"
        return null == date ? "1970-01-01" : DATE_FORMATTER.format(date);
    }
}
