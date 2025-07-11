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
package liquibase.ext.clickhouse.sqlgenerator.changelog.template;

import liquibase.database.Database;
import liquibase.ext.clickhouse.params.LiquibaseClickHouseConfig;
import liquibase.ext.clickhouse.params.StandaloneConfig;
import liquibase.ext.clickhouse.sqlgenerator.LiquibaseSqlTemplate;
import liquibase.ext.clickhouse.sqlgenerator.changelog.ChangelogColumns;

public class TagDatabaseGeneratorTemplate extends LiquibaseSqlTemplate<String> {

    private final Database database;
    private final String tagName;

    public TagDatabaseGeneratorTemplate(
        Database database, String tagName
    ) {
        this.database = database;
        this.tagName = tagName;
    }

    @Override
    public String visitDefault(LiquibaseClickHouseConfig object) {
        boolean isStandalone = object instanceof StandaloneConfig;
        String alteredColumns = String.format("%s = %s", ChangelogColumns.TAG, escape(database, tagName));
        String whereColumns = String.format("%s = %s", ChangelogColumns.ID, getIdSubQuery(isStandalone));
        return String.format(
            "ALTER TABLE %s.%s UPDATE %s WHERE %s",
            database.getLiquibaseCatalogName(),
            database.getDatabaseChangeLogTableName(),
            alteredColumns,
            whereColumns
        );
    }

    private String getIdSubQuery(boolean isStandalone) {
        // Only use FINAL keyword for standalone mode with ReplacingMergeTree engine (non-legacy)
        boolean useFinal = isStandalone && !liquibase.ext.clickhouse.params.ParamsLoader.useLegacyStorage();
        return String.format(
            "(SELECT %s FROM %s.%s %s ORDER BY %s DESC, %s DESC LIMIT 1)",
            ChangelogColumns.ID,
            database.getLiquibaseCatalogName(),
            database.getDatabaseChangeLogTableName(),
            useFinal ? "FINAL" : "",
            ChangelogColumns.DATEEXECUTED,
            ChangelogColumns.ORDEREXECUTED
        );
    }
}
