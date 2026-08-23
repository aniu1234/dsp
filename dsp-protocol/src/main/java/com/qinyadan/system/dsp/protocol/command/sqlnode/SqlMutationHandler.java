package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.core.util.StringUtil;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.service.MutationService;
import com.qinyadan.system.dsp.engine.service.WriteErrorCode;
import com.qinyadan.system.dsp.engine.service.WriteServiceException;
import com.qinyadan.system.dsp.engine.service.dto.MutationAssignment;
import com.qinyadan.system.dsp.engine.service.dto.MutationOperation;
import com.qinyadan.system.dsp.engine.service.dto.MutationOutcome;
import com.qinyadan.system.dsp.engine.service.dto.MutationRequest;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import org.apache.calcite.sql.SqlDelete;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlUpdate;
import org.apache.calcite.sql.SqlUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.INCORRECT_COLUMN_VALUE;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.INTERNAL_ERROR;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.COLUMN_CANNOT_BE_NULL;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.COLUMN_EXIST_TWICE;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.NO_DATABASE_SELECTED;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.TABLE_NOT_EXISTS;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.UNKNOWN_DB_NAME;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.UNSUPPORTED_FEATURE;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.UNKONW_COLUMN_NAME;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.WRITE_RESOURCE_LIMIT;

/**
 * MySQL protocol adapter for P2.1 autocommit UPDATE and DELETE statements.
 */
public final class SqlMutationHandler implements Handler<SqlNode> {

    public static final SqlMutationHandler INSTANCE = new SqlMutationHandler();

    private SqlMutationHandler() {
    }

    @Override
    public void handle(ConnectionContext context, SqlNode statement) {
        SqlNode target;
        String alias;
        String condition;
        MutationOperation operation;
        List<MutationAssignment> assignments;
        if (statement instanceof SqlUpdate) {
            SqlUpdate update = (SqlUpdate) statement;
            target = update.getTargetTable();
            alias = identifier(update.getAlias());
            condition = update.getCondition() == null ? null : update.getCondition().toString();
            operation = MutationOperation.UPDATE;
            assignments = assignments(update);
        } else if (statement instanceof SqlDelete) {
            SqlDelete delete = (SqlDelete) statement;
            target = delete.getTargetTable();
            alias = identifier(delete.getAlias());
            condition = delete.getCondition() == null ? null : delete.getCondition().toString();
            operation = MutationOperation.DELETE;
            assignments = Collections.emptyList();
        } else {
            throw new IllegalArgumentException("Unsupported mutation statement: " + statement);
        }

        Pair<String, String> resolved = StringUtil.getDbAndTablePair(
                target.toString(), context.getDb());
        String database = resolved.getLeft();
        String table = resolved.getRight();
        if (database == null) {
            context.write(PackageUtils.buildErrPackage(
                    NO_DATABASE_SELECTED.getCode(), NO_DATABASE_SELECTED.getMessage()));
            return;
        }
        if (!CatalogService.INSTANCE.databaseExists(database)) {
            context.write(PackageUtils.buildErrPackage(
                    UNKNOWN_DB_NAME.getCode(),
                    String.format(UNKNOWN_DB_NAME.getMessage(), database)));
            return;
        }

        try {
            MutationOutcome outcome = MutationService.INSTANCE.mutate(
                    new MutationRequest(operation, database, table, alias,
                            condition, assignments));
            context.write(PackageUtils.buildOkMySqlPackage(
                    outcome.getAffectedRows(), 1, 0));
        } catch (WriteServiceException e) {
            writeError(context, database, table, e);
        }
    }

    private List<MutationAssignment> assignments(SqlUpdate update) {
        List<SqlNode> columns = update.getTargetColumnList().getList();
        List<SqlNode> expressions = update.getSourceExpressionList().getList();
        if (columns.size() != expressions.size()) {
            throw new IllegalArgumentException("UPDATE assignment count does not match");
        }
        List<MutationAssignment> result = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            SqlNode expression = expressions.get(i);
            result.add(new MutationAssignment(identifier(columns.get(i)),
                    expression.toString(), expression.getKind() == SqlKind.DEFAULT,
                    SqlUtil.isNullLiteral(expression, false)));
        }
        return result;
    }

    private String identifier(SqlNode node) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlIdentifier && ((SqlIdentifier) node).isSimple()) {
            return ((SqlIdentifier) node).getSimple();
        }
        return node.toString();
    }

    private void writeError(ConnectionContext context, String database, String table,
                            WriteServiceException error) {
        WriteErrorCode code = error.getErrorCode();
        if (code == WriteErrorCode.UNKNOWN_TABLE) {
            context.write(PackageUtils.buildErrPackage(TABLE_NOT_EXISTS.getCode(),
                    String.format(TABLE_NOT_EXISTS.getMessage(), database + "." + table)));
        } else if (code == WriteErrorCode.RESOURCE_LIMIT) {
            context.write(PackageUtils.buildErrPackage(WRITE_RESOURCE_LIMIT.getCode(),
                    String.format(WRITE_RESOURCE_LIMIT.getMessage(), error.getMessage())));
        } else if (code == WriteErrorCode.UNSUPPORTED_OPERATION) {
            context.write(PackageUtils.buildErrPackage(UNSUPPORTED_FEATURE.getCode(),
                    String.format(UNSUPPORTED_FEATURE.getMessage(), error.getMessage())));
        } else if (code == WriteErrorCode.STORAGE_FAILURE) {
            context.write(PackageUtils.buildErrPackage(INTERNAL_ERROR.getCode(),
                    String.format(INTERNAL_ERROR.getMessage(), error.getMessage())));
        } else if (code == WriteErrorCode.UNKNOWN_COLUMN) {
            context.write(PackageUtils.buildErrPackage(UNKONW_COLUMN_NAME.getCode(),
                    String.format(UNKONW_COLUMN_NAME.getMessage(), detailValue(error))));
        } else if (code == WriteErrorCode.DUPLICATE_COLUMN) {
            context.write(PackageUtils.buildErrPackage(COLUMN_EXIST_TWICE.getCode(),
                    String.format(COLUMN_EXIST_TWICE.getMessage(), detailValue(error))));
        } else if (code == WriteErrorCode.CONSTRAINT_VIOLATION
                && error.getMessage() != null
                && error.getMessage().contains("cannot be null")) {
            context.write(PackageUtils.buildErrPackage(COLUMN_CANNOT_BE_NULL.getCode(),
                    String.format(COLUMN_CANNOT_BE_NULL.getMessage(), quotedValue(error))));
        } else {
            context.write(PackageUtils.buildErrPackage(INCORRECT_COLUMN_VALUE.getCode(),
                    String.format(INCORRECT_COLUMN_VALUE.getMessage(),
                            "expression", "mutation", 1)));
        }
    }

    private String detailValue(WriteServiceException error) {
        String message = error.getMessage();
        int separator = message == null ? -1 : message.lastIndexOf(": ");
        return separator < 0 ? String.valueOf(message) : message.substring(separator + 2);
    }

    private String quotedValue(WriteServiceException error) {
        String message = error.getMessage();
        int start = message == null ? -1 : message.indexOf('\'');
        int end = start < 0 ? -1 : message.indexOf('\'', start + 1);
        return start < 0 || end < 0 ? "mutation" : message.substring(start + 1, end);
    }
}
