package com.qinyadan.system.dsp.storage.api.query;

import java.io.Serializable;
import java.util.Set;

/**
 * Query condition abstraction.
 */
public interface Query extends Serializable {
    String getSql();

    Set<String> getColumnNames();
}
