/*-
 * #%L
 * Liquibase extension for ClickHouse
 * %%
 * Copyright (C) 2020 - 2023 Mediarithmics
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
package liquibase;

import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.ext.clickhouse.params.LiquibaseClickHouseConfig;
import liquibase.ext.clickhouse.params.ParamsLoader;
import liquibase.ext.clickhouse.sqlgenerator.changelog.ChangelogColumns;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.apache.commons.io.output.NullWriter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class BaseClickHouseTestCase {
    @Test
    void canInitializeLiquibaseSchema() {
        runLiquibase("empty-changelog.xml", (liquibase, connection) -> liquibase.update());
    }

    @Test
    void canExecuteChangelog() {
        // Expected single row from
        runLiquibase(
            getChangelogFileName(),
            (liquibase, connection) -> {
                liquibase.update();
                liquibase.update(); // Test that successive updates are working
            }
        );
    }

    @Test
    void canRollbackChangelog() {
        runLiquibase(
            getChangelogFileName(),
            (liquibase, connection) -> {
                liquibase.update();
                liquibase.rollback(Date.from(Instant.EPOCH), "");
            }
        );
    }

    @Test
    void canTagDatabase() {
        runLiquibase(
            getChangelogFileName(),
            (liquibase, connection) -> {
                liquibase.update();
                liquibase.tag("testTag");
            }
        );
    }

    @Test
    void canValidate() {
        runLiquibase(getChangelogFileName(), (liquibase, connection) -> liquibase.validate());
    }

    @Test
    void canListLocks() {
        runLiquibase(getChangelogFileName(), (liquibase, connection) -> liquibase.listLocks());
    }

    @Test
    void canSyncChangelog() {
        // ERROR: Exception Primary Reason: Result set larger than one row
        runLiquibase(getChangelogFileName(), (liquibase, connection) -> liquibase.changeLogSync(""));
    }

    @Test
    void canForceReleaseLocks() {
        runLiquibase(getChangelogFileName(), (liquibase, connection) -> liquibase.forceReleaseLocks());
    }

    @Test
    void canReportStatus() {
        runLiquibase(
            getChangelogFileName(),
            (liquibase, connection) -> liquibase.reportStatus(true, "", new NullWriter())
        );
    }

    @Test
    void canMarkChangeSetRan() {
        runLiquibase(getChangelogFileName(), (liquibase, connection) -> liquibase.markNextChangeSetRan(""));
    }

    @Test
    void canUpdateCheckSums() {
        @Language("ClickHouse")
        final String clearCheckSum = "ALTER TABLE DATABASECHANGELOG UPDATE MD5SUM = '' WHERE TRUE";
        runLiquibase(
            getChangelogFileName(),
            (liquibase, connection) -> {
                liquibase.update("");
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(clearCheckSum);
                }
                liquibase.update("");
            }
        );
    }

    @Test
    void canExecuteRunAlwaysChangeSetTwice() {
        runLiquibase(
            getChangelogFileName(), (liquibase, connection) -> {
                // The first run should execute the runAlways changeSet
                liquibase.update("");
                String runAlwaysChangeSetId = getRunAlwaysChangeSetId();
                var runAlwaysChangeSet = getChangeLogRow(runAlwaysChangeSetId, connection);
                assertFalse(runAlwaysChangeSet.get(ChangelogColumns.MD5SUM).toString().isEmpty());
                // Check that the changeSet was executed, the value is either
                // EXECUTED (in case of a first run) or RERAN (in case other testcases were run before)
                assertTrue(List.of(ChangeSet.ExecType.EXECUTED.name(), ChangeSet.ExecType.RERAN.name())
                    .contains(runAlwaysChangeSet.get(ChangelogColumns.EXECTYPE)));
                assertTrue(runAlwaysChangeSet.get(ChangelogColumns.COMMENTS).toString()
                    .endsWith("Inserting some data on each run..."));

                // Manually update DATABASECHANGELOG to simulate a change
                @Language("ClickHouse")
                String updateSql = "ALTER TABLE DATABASECHANGELOG UPDATE MD5SUM = ?, COMMENTS = ? WHERE ID = ?";
                try (var pstmt = connection.prepareStatement(updateSql)) {
                    pstmt.setString(1, "");
                    pstmt.setString(2, "sample comment");
                    pstmt.setString(3, runAlwaysChangeSetId);
                    pstmt.execute();
                }
                Thread.sleep(2000); //ensure the changes are visible
                runAlwaysChangeSet = getChangeLogRow(runAlwaysChangeSetId, connection);
                assertEquals("", runAlwaysChangeSet.get(ChangelogColumns.MD5SUM));
                assertEquals("sample comment", runAlwaysChangeSet.get(ChangelogColumns.COMMENTS));

                // The second run should execute the runAlways changeSet again
                liquibase.update();
                // Wait for async ALTER TABLE UPDATE to complete in ClickHouse
                Thread.sleep(2000);
                runAlwaysChangeSet = getChangeLogRow(runAlwaysChangeSetId, connection);
                assertFalse(runAlwaysChangeSet.get(ChangelogColumns.MD5SUM).toString().isEmpty());
                assertEquals(ChangeSet.ExecType.RERAN.name(), runAlwaysChangeSet.get(ChangelogColumns.EXECTYPE));
                // COMMENTS should be updated when runAlways changeSet is re-executed
                assertTrue(runAlwaysChangeSet.get(ChangelogColumns.COMMENTS).toString()
                    .endsWith("Inserting some data on each run..."));
            }
        );
    }

    private static Map<ChangelogColumns, Object> getChangeLogRow(String id, Connection connection)
        throws SQLException {
        @Language("ClickHouse")
        String sql = "SELECT * FROM DATABASECHANGELOG WHERE ID = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<ChangelogColumns, Object> row = new EnumMap<>(ChangelogColumns.class);
                    for (ChangelogColumns column : ChangelogColumns.values()) {
                        row.put(column, rs.getObject(column.toString()));
                    }
                    return row;
                } else {
                    throw new RuntimeException("No row found for ID = " + id);
                }
            }
        }
    }

    protected abstract void doWithConnection(ThrowingConsumer<Connection> callback);

    protected abstract String getChangelogFileName();

    protected abstract String getRunAlwaysChangeSetId();

    void runLiquibase(
        String changelog, ThrowingBiConsumer<Liquibase, Connection> liquibaseAction
    ) {
        DatabaseFactory dbFactory = DatabaseFactory.getInstance();
        try (ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor()) {

        doWithConnection(
            connection -> {
                JdbcConnection jdbcConnection = new JdbcConnection(connection);
                Database database = dbFactory.findCorrectDatabaseImplementation(jdbcConnection);
                database.setLiquibaseSchemaName("default");
                database.setLiquibaseCatalogName("default");
                Liquibase liquibase = new Liquibase(changelog, resourceAccessor, database);
                liquibaseAction.accept(liquibase, connection);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setConfig(LiquibaseClickHouseConfig config)
        throws NoSuchFieldException, IllegalAccessException {
        var f = ParamsLoader.class.getDeclaredField("liquibaseClickhouseProperties");
        f.setAccessible(true);
        f.set(null, config);
    }

    @FunctionalInterface
    protected interface ThrowingBiConsumer<T1, T2> {
        void accept(T1 t1, T2 t2) throws Exception;
    }

    @FunctionalInterface
    protected interface ThrowingConsumer<T1> {
        void accept(T1 t1) throws Exception;
    }
}
