package com.qinyadan.system.dsp.schema.common.integrate;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.schema.common.util.ResultSetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.InternalProperty;
import org.apache.calcite.avatica.util.Casing;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.CALCITE_URL;
import static com.qinyadan.system.dsp.schema.common.constants.MetaConstants.META_MODEL;


@Slf4j
public abstract class IntegrateTestBase {

    private static final Pattern SQL_START = Pattern.compile(
            "(?i)^(select|with|create|set|insert|update|delete|drop|alter|merge|values|explain|show|use)\\b.*");

    protected Statement calciteStatement;

    public IntegrateTestBase() {
        getStatement();
    }

    protected boolean isEmptyLineOrComment(String line) {
        if (Objects.isNull(line)) {
            return false;
        }

        final String sql = line.trim();
        return !(StringUtils.isEmpty(line) || sql.startsWith("--") || sql.startsWith("#"));
    }

    protected List<String> loadSqlStatements(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("SQL resource does not exist");
        }

        List<String> statements = Lists.newArrayList();
        StringBuilder current = new StringBuilder();
        for (String line : IOUtils.readLines(inputStream, StandardCharsets.UTF_8)) {
            String sql = stripComment(line).trim();
            if (sql.isEmpty()) {
                continue;
            }

            if (SQL_START.matcher(sql).matches()) {
                addStatement(statements, current);
            } else if (current.length() == 0) {
                log.debug("Ignore non-SQL resource line: {}", sql);
                continue;
            }

            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(sql);
        }
        addStatement(statements, current);
        return statements;
    }

    private static String stripComment(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
            return "";
        }
        int comment = line.indexOf("--");
        int hashComment = line.indexOf('#');
        if (comment < 0 || hashComment >= 0 && hashComment < comment) {
            comment = hashComment;
        }
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        if (current.length() > 0) {
            statements.add(current.toString());
            current.setLength(0);
        }
    }

    private void getStatement() {
        try {
            if (calciteStatement != null && !calciteStatement.isClosed()) {
                return;
            }

            Properties properties = getProperties();
            Connection connection =
                    DriverManager.getConnection(CALCITE_URL, properties);

            calciteStatement = connection.createStatement();
        } catch (Exception e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    protected Properties getProperties() {
        final Properties info = new Properties();
        //FIXME should not use hard code — use classpath-relative resource
        info.setProperty(META_MODEL,
                Thread.currentThread().getContextClassLoader()
                        .getResource("schema.json").getPath());

        info.setProperty(InternalProperty.CASE_SENSITIVE.name(), "false");
        info.setProperty(InternalProperty.UNQUOTED_CASING.name(), Casing.UNCHANGED.name());
        return info;
    }

    protected boolean compareResultIgnoreSequence(List<List<String>> expect, List<List<String>> actual) {
        //sort
        expect.sort(Comparator.comparing(Object::toString));
        actual.sort(Comparator.comparing(Object::toString));

        final int expectLen = expect.size();
        final int acutalLen = actual.size();

        if (expectLen != acutalLen) {
            return true;
        }

        for (int i = 0; i < acutalLen; i++) {
            if (!Objects.equals(expect.get(i), actual.get(i))) {
                return true;
            }
        }

        return false;
    }

    protected List<List<String>> runSql(String sql, Statement statement) {

        final List<List<String>> result = Lists.newArrayList();
        try {
            final ResultSet r = statement.executeQuery(sql);
            final int columnCount = r.getMetaData().getColumnCount();
            final List<Class> columnType = ResultSetUtils.getColumnTypeFromResultSet(r);
            while (r.next()) {
                final List<String> value = Lists.newArrayListWithCapacity(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    //FIXME r.getString is not exact
                    //value.add(r.getString(i));
                    value.add(ResultSetUtils.javaTypeToString(r, i, columnType.get(i - 1)));
                }
                result.add(value);
            }
        } catch (Exception e) {
            log.error(e.toString());
            throw new RuntimeException(Throwables.getStackTraceAsString(e));
        }

        return result;
    }


    protected List<List<String>> createExpectResult(String origin) {
        return Arrays.stream(origin.split(";")).map(string ->
                        Arrays.stream(string.split(","))
                                .map(String::trim).map(s -> {
                                    if (s.equalsIgnoreCase("null")) {
                                        return null;
                                    }

                                    return s;
                                }).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
