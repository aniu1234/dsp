package com.qinyadan.system.dsp.constant;


public enum ErrorCodeAndMessageEnum {

    /**
     * User or password is wrong
     */
    PASSWORD_OR_USER_IS_WRONG(1045, "Access denied: wrong username or password"),

    /**
     * drop database xxx and xxx does not exsits
     */
    DATABASE_NOT_EXIST_IN_DROP(1007, "Can't drop database '%s'; database doesn't exist"),

    /**
     * database exists when create database exists
     */
    DATABASE_EXISTS_ERROR(1007, "Can't create database '%s'; database exists"),

    /**
     * When create table, no db select
     */
    NO_DATABASE_SELECTED(1046, "No database selected"),


    /**
     * When create table
     * <p>
     * create tabel `db.t` and db does not exist
     */
    UNKNOWN_DB_NAME(1049, "Unknown database '%s'"),


    /**
     * when create table t and t has already existed
     */
    TABLE_ALREADY_EXISTS(1050, "Table '%s' already exists"),

    /**
     * Drop table 'test.test', if test or test does not exists
     */
    UNKNOWN_TABLE_NAME(1051, "Unknown table '%s'"),

    /**
     * Unknwn column name
     */
    UNKONW_COLUMN_NAME(1054, "Unknown column '%s' in 'field list'"),

    /**
     * A CREATE TABLE statement declares the same column more than once.
     */
    DUPLICATE_COLUMN_NAME(1060, "Duplicate column name '%s'"),

    /**
     * A null value was written to a NOT NULL column.
     */
    COLUMN_CANNOT_BE_NULL(1048, "Column '%s' cannot be null"),

    /**
     * Do not support this syntax
     */
    SYNTAX_ERROR(1064,
            "You have an error in your SQL syntax; check the manual"
                    + " that corresponds to your MySQL server version for the right syntax to use near '%s'"),

    /**
     * insert into t(c1, c2) value(....)
     */
    COLUMN_EXIST_TWICE(1110, "Column '%s' specified twice"),

    /**
     * insert into student(h,t) values(1); will encounter this problem
     */
    COLUMN_COUNT_NOT_MATCH(1136, "Column count doesn't match value count at row %s"),

    /**
     * A literal cannot be converted to the declared column type.
     */
    INCORRECT_COLUMN_VALUE(1366, "Incorrect value '%s' for column '%s' at row %s"),

    /**
     * The client sent a protocol command this server does not implement.
     */
    UNKNOWN_COMMAND(1047, "Unknown command"),

    /**
     * The SQL syntax is valid, but the feature is outside the supported surface.
     */
    UNSUPPORTED_FEATURE(1235, "This version of DSP doesn't yet support '%s'"),

    /**
     * Query resource guard triggered before the process ran out of memory.
     */
    QUERY_RESOURCE_LIMIT(1226, "Query resource limit exceeded: %s"),

    /**
     * A single write statement exceeded its configured row limit.
     */
    WRITE_RESOURCE_LIMIT(1226, "Write resource limit exceeded: %s"),

    /**
     * Query exceeded its configured execution deadline.
     */
    QUERY_TIMEOUT(3024, "Query execution was interrupted after exceeding %s ms"),

    /**
     * An unexpected execution error occurred.
     */
    INTERNAL_ERROR(1105, "Query execution failed: %s"),

    /**
     * Table do not exist
     */
    TABLE_NOT_EXISTS(1146, "Table '%s' doesn't exist");


    private final int code;
    private final String message;

    ErrorCodeAndMessageEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
