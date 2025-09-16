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

import liquibase.ChecksumVersion;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.ext.clickhouse.database.ClickHouseDatabase;
import liquibase.ext.clickhouse.params.ParamsLoader;
import liquibase.ext.clickhouse.sqlgenerator.SqlGeneratorUtil;
import liquibase.ext.clickhouse.sqlgenerator.changelog.template.UpdateTemplate;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.MarkChangeSetRanGenerator;
import liquibase.statement.DatabaseFunction;
import liquibase.statement.core.MarkChangeSetRanStatement;
import liquibase.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.Set;

public class MarkChangeSetRanGeneratorClickhouse extends MarkChangeSetRanGenerator {

    private static int getOrderExecutedColumn(Database database) {
        try {
            return Scope.getCurrentScope().getSingleton(ChangeLogHistoryServiceFactory.class)
                .getChangeLogService(database).getNextSequenceValue();
        } catch (LiquibaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    private static String getCommentsColumn(ChangeSet changeSet) {
        return StringUtil.limitSize(StringUtils.trimToEmpty(changeSet.getComments()), 250);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(MarkChangeSetRanStatement statement, Database database) {
        return database instanceof ClickHouseDatabase;
    }

    @Override
    public Sql[] generateSql(
        MarkChangeSetRanStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain
    ) {
        var execType = statement.getExecType();
        if (Set.of(ChangeSet.ExecType.FAILED, ChangeSet.ExecType.SKIPPED).contains(execType) || !execType.ranBefore) {
            return super.generateSql(statement, database, sqlGeneratorChain);
        }
        // dealing with an update case
        ChangeSet changeSet = statement.getChangeSet();
        var config = ParamsLoader.getLiquibaseClickhouseProperties();
        var map = new EnumMap<>(ChangelogColumns.class);
        map.put(ChangelogColumns.DATEEXECUTED, new DatabaseFunction(database.getCurrentDateTimeFunction()));
        map.put(ChangelogColumns.ORDEREXECUTED, getOrderExecutedColumn(database));
        map.put(ChangelogColumns.MD5SUM, changeSet.generateCheckSum(ChecksumVersion.latest()).toString());
        map.put(ChangelogColumns.EXECTYPE, execType.value);
        map.put(ChangelogColumns.DEPLOYMENT_ID, Scope.getCurrentScope().getDeploymentId());
        map.put(ChangelogColumns.COMMENTS, getCommentsColumn(changeSet));
        map.put(ChangelogColumns.CONTEXTS, getContextsColumn(changeSet));
        map.put(ChangelogColumns.LABELS, getLabelsColumn(changeSet));
        map.put(ChangelogColumns.LIQUIBASE, getLiquibaseBuildVersion());
        map.put(ChangelogColumns.DESCRIPTION, StringUtil.limitSize(changeSet.getDescription(), 250));
        map.put(ChangelogColumns.TAG, getTagFromChangeset(changeSet));
        var query = config.accept(new UpdateTemplate(database, map, changeSet.getId()));
        return SqlGeneratorUtil.generateSql(database, query);
    }
}
