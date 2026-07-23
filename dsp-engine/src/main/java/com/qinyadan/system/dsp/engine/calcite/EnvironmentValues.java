package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.collect.Maps;

import java.util.Map;


public class EnvironmentValues {
    public static final Map<String, String> GLOBAL_ENVIRONMENT = Maps.newHashMap();

    static {
        GLOBAL_ENVIRONMENT.put("version_comment", "Create by DSP");
        GLOBAL_ENVIRONMENT.put("version", "5.7.22-dsp");
        GLOBAL_ENVIRONMENT.put("autocommit", "1");
        GLOBAL_ENVIRONMENT.put("transaction_isolation", "READ-COMMITTED");
        GLOBAL_ENVIRONMENT.put("tx_isolation", "READ-COMMITTED");
        GLOBAL_ENVIRONMENT.put("transaction_read_only", "0");
        GLOBAL_ENVIRONMENT.put("tx_read_only", "0");
        GLOBAL_ENVIRONMENT.put("sql_mode", "");
        GLOBAL_ENVIRONMENT.put("time_zone", "+00:00");
        GLOBAL_ENVIRONMENT.put("character_set_client", "utf8mb4");
        GLOBAL_ENVIRONMENT.put("character_set_connection", "utf8mb4");
        GLOBAL_ENVIRONMENT.put("character_set_results", "utf8mb4");
        GLOBAL_ENVIRONMENT.put("character_set_server", "utf8mb4");
        GLOBAL_ENVIRONMENT.put("collation_connection", "utf8mb4_general_ci");
        GLOBAL_ENVIRONMENT.put("collation_server", "utf8mb4_general_ci");
        GLOBAL_ENVIRONMENT.put("lower_case_table_names", "0");
        GLOBAL_ENVIRONMENT.put("max_allowed_packet", "67108864");
        GLOBAL_ENVIRONMENT.put("auto_increment_increment", "1");
        GLOBAL_ENVIRONMENT.put("init_connect", "");
        GLOBAL_ENVIRONMENT.put("interactive_timeout", "28800");
        GLOBAL_ENVIRONMENT.put("license", "Apache-2.0");
        GLOBAL_ENVIRONMENT.put("net_write_timeout", "60");
        GLOBAL_ENVIRONMENT.put("performance_schema", "0");
        GLOBAL_ENVIRONMENT.put("query_cache_size", "0");
        GLOBAL_ENVIRONMENT.put("query_cache_type", "OFF");
        GLOBAL_ENVIRONMENT.put("system_time_zone", "UTC");
        GLOBAL_ENVIRONMENT.put("wait_timeout", "28800");
    }
}
