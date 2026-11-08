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
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.ext.clickhouse.database.ClickHouseDatabase;
import liquibase.ext.clickhouse.sqlgenerator.SqlGeneratorUtil;
import liquibase.ext.clickhouse.sqlgenerator.changelog.template.UpdateTemplate;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.UpdateChangeSetChecksumGenerator;
import liquibase.statement.core.UpdateChangeSetChecksumStatement;

import java.util.EnumMap;

public class UpdateChangeSetChecksumClickHouse extends UpdateChangeSetChecksumGenerator {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(UpdateChangeSetChecksumStatement statement, Database database) {
        return database instanceof ClickHouseDatabase;
    }

    @Override
    public Sql[] generateSql(
        UpdateChangeSetChecksumStatement statement,
        Database database,
        SqlGeneratorChain sqlGeneratorChain
    ) {
        ChangeSet changeSet = statement.getChangeSet();
        var config = ((ClickHouseDatabase) database).getLiquibaseClickHouseConfig();

        var map = new EnumMap<>(ChangelogColumns.class);
        // author and filename can't be changed, as they are part of the primary key
        map.put(ChangelogColumns.MD5SUM, changeSet.generateCheckSum(ChecksumVersion.latest()).toString());
        var query = config.accept(new UpdateTemplate(database, map, changeSet.getId()));
        return SqlGeneratorUtil.generateSql(database, query);
    }
}
