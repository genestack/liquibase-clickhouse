/*-
 * #%L
 * Liquibase extension for ClickHouse
 * %%
 * Copyright (C) 2024 - 2025 Genestack Limited
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package liquibase.ext.clickhouse.sqlgenerator.changelog;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.ext.clickhouse.database.ClickHouseDatabase;
import liquibase.ext.clickhouse.params.StandaloneConfig;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.UpdateGenerator;
import liquibase.statement.DatabaseFunction;
import liquibase.statement.core.UpdateStatement;

import java.util.Date;

import static liquibase.util.SqlUtil.replacePredicatePlaceholders;

public class UpdateGeneratorClickhouse extends UpdateGenerator {
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(UpdateStatement statement, Database database) {
        return database instanceof ClickHouseDatabase;
    }

    @Override
    public Sql[] generateSql(
        UpdateStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain
    ) {
        var config = ((ClickHouseDatabase) database).getLiquibaseClickHouseConfig();
        if (config instanceof StandaloneConfig) {
            return super.generateSql(statement, database, sqlGeneratorChain);
        }
        return generateSqlForCluster(statement, database);
    }

    private Sql[] generateSqlForCluster(UpdateStatement statement, Database database) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ")
            .append(database.escapeTableName(
                statement.getCatalogName(), statement.getSchemaName(),
                statement.getTableName()
            ))
            .append(" UPDATE");
        for (String column: statement.getNewColumnValues().keySet()) {
            sql.append(" ")
                .append(database.escapeColumnName(
                    statement.getCatalogName(), statement.getSchemaName(),
                    statement.getTableName(), column
                ))
                .append(" = ")
                .append(convertToString(statement.getNewColumnValues().get(column), database))
                .append(",");
        }

        int lastComma = sql.lastIndexOf(",");
        if (lastComma >= 0) {
            sql.deleteCharAt(lastComma);
        }
        if (statement.getWhereClause() != null) {
            sql.append(" WHERE ").append(
                replacePredicatePlaceholders(
                    database, statement.getWhereClause(), statement.getWhereColumnNames(),
                    statement.getWhereParameters()
                ));
        }

        return new Sql[] {
            new UnparsedSql(sql.toString(), getAffectedTable(statement))
        };
    }

    /**
     * Copied as is from {@link UpdateGenerator}
     */
    private String convertToString(Object newValue, Database database) {
        String sqlString;
        if ((newValue == null) || "NULL".equalsIgnoreCase(newValue.toString())) {
            sqlString = "NULL";
        } else if ((newValue instanceof String) && !looksLikeFunctionCall(((String) newValue), database)) {
            sqlString = DataTypeFactory.getInstance().fromObject(newValue, database).objectToSql(newValue, database);
        } else if (newValue instanceof Date) {
            // converting java.util.Date to java.sql.Date
            Date date = (Date) newValue;
            if (date.getClass().equals(java.util.Date.class)) {
                date = new java.sql.Date(date.getTime());
            }

            sqlString = database.getDateLiteral(date);
        } else if (newValue instanceof Boolean) {
            if (((Boolean) newValue)) {
                sqlString = DataTypeFactory.getInstance().getTrueBooleanValue(database);
            } else {
                sqlString = DataTypeFactory.getInstance().getFalseBooleanValue(database);
            }
        } else if (newValue instanceof DatabaseFunction) {
            sqlString = database.generateDatabaseFunctionValue((DatabaseFunction) newValue);
        } else {
            sqlString = newValue.toString();
        }
        return sqlString;
    }
}
