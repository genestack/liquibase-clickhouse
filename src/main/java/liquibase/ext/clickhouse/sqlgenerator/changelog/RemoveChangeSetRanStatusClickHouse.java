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
import liquibase.ext.clickhouse.database.ClickHouseDatabase;
import liquibase.ext.clickhouse.params.LiquibaseClickHouseConfig;
import liquibase.ext.clickhouse.sqlgenerator.SqlGeneratorUtil;
import liquibase.ext.clickhouse.sqlgenerator.changelog.template.RemoveChangeSetRanStatusTemplate;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.RemoveChangeSetRanStatusGenerator;
import liquibase.statement.core.RemoveChangeSetRanStatusStatement;

public class RemoveChangeSetRanStatusClickHouse extends RemoveChangeSetRanStatusGenerator {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(RemoveChangeSetRanStatusStatement statement, Database database) {
        return database instanceof ClickHouseDatabase;
    }

    @Override
    public Sql[] generateSql(
        RemoveChangeSetRanStatusStatement statement,
        Database database,
        SqlGeneratorChain sqlGeneratorChain
    ) {
        LiquibaseClickHouseConfig properties = ((ClickHouseDatabase) database).getLiquibaseClickHouseConfig();
        String removeChangeSet =
            properties.accept(new RemoveChangeSetRanStatusTemplate(database, statement));
        return SqlGeneratorUtil.generateSql(database, removeChangeSet);
    }
}
