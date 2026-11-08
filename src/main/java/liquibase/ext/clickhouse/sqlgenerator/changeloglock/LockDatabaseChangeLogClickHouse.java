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
package liquibase.ext.clickhouse.sqlgenerator.changeloglock;

import liquibase.database.Database;
import liquibase.ext.clickhouse.database.ClickHouseDatabase;
import liquibase.ext.clickhouse.sqlgenerator.SqlGeneratorUtil;
import liquibase.ext.clickhouse.sqlgenerator.changeloglock.template.LockTemplate;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.LockDatabaseChangeLogGenerator;
import liquibase.statement.core.LockDatabaseChangeLogStatement;

public class LockDatabaseChangeLogClickHouse extends LockDatabaseChangeLogGenerator {

    public LockDatabaseChangeLogClickHouse() {
        super();
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(LockDatabaseChangeLogStatement statement, Database database) {
        return database instanceof ClickHouseDatabase;
    }

    @Override
    public Sql[] generateSql(
        LockDatabaseChangeLogStatement statement,
        Database database,
        SqlGeneratorChain sqlGeneratorChain
    ) {
        var config = ((ClickHouseDatabase) database).getLiquibaseClickHouseConfig();
        String host = String.format("%s %s (%s)", hostname, hostDescription, hostaddress);
        String lockQuery = config.accept(new LockTemplate(database, host));
        return SqlGeneratorUtil.generateSql(database, lockQuery);
    }
}
