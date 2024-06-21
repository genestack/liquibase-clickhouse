/*-
 * #%L
 * Liquibase extension for ClickHouse
 * %%
 * Copyright (C) 2020 - 2024 Genestack LTD
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
import liquibase.ext.clickhouse.params.ParamsLoader;
import liquibase.ext.clickhouse.sqlgenerator.SqlGeneratorUtil;
import liquibase.ext.clickhouse.sqlgenerator.changeloglock.template.SelectLockTemplate;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.SelectFromDatabaseChangeLogLockGenerator;
import liquibase.statement.core.SelectFromDatabaseChangeLogLockStatement;

public class SelectFromDatabaseChangeLogLockGeneratorClickHouse
    extends SelectFromDatabaseChangeLogLockGenerator {

  @Override
  public int getPriority() {
    return PRIORITY_DATABASE;
  }

  @Override
  public Sql[] generateSql(
      SelectFromDatabaseChangeLogLockStatement statement,
      final Database database,
      SqlGeneratorChain sqlGeneratorChain) {
    var config = ParamsLoader.getLiquibaseClickhouseProperties();
    String query = config.accept(new SelectLockTemplate(database, statement));
    return SqlGeneratorUtil.generateSql(database, query);
  }
}
