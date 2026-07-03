package com.qinyadan.system.dsp.storage;

import lombok.Builder;
import lombok.Data;

/**
 *
 */
@Data
@Builder
public class BaseQuery implements Query {
    private String query;
}
