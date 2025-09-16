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
import liquibase.ext.clickhouse.sqlgenerator.LiquibaseSqlTemplate;
import liquibase.ext.clickhouse.sqlgenerator.changelog.ChangelogColumns;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateTemplate extends LiquibaseSqlTemplate<String> {

    private final Database database;
    private final Map<ChangelogColumns, Object> replacements;
    private final String unescapedId;

    public UpdateTemplate(
        Database database, Map<ChangelogColumns, Object> replacements, String unescapedId
    ) {
        this.database = database;
        this.replacements = Collections.unmodifiableMap(replacements);
        this.unescapedId = unescapedId;
    }

    @Override
    public String visitDefault(LiquibaseClickHouseConfig object) {
        String alteredColumns =
            replacements.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(
                    entry ->
                        String.format("%s = %s", entry.getKey(), escape(database, entry.getValue())))
                .collect(Collectors.joining(", "));
        String whereColumns = String.format("ID = '%s'", unescapedId);
        return String.format(
            "ALTER TABLE %s.%s UPDATE %s WHERE %s",
            database.getLiquibaseCatalogName(),
            database.getDatabaseChangeLogTableName(),
            alteredColumns,
            whereColumns
        );
    }
}
