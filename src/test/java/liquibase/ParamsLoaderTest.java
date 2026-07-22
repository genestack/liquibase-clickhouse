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

import liquibase.ext.clickhouse.params.ClusterConfig;
import liquibase.ext.clickhouse.params.LiquibaseClickHouseConfig;
import liquibase.ext.clickhouse.params.ParamsLoader;
import liquibase.ext.clickhouse.params.StandaloneConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ParamsLoaderTest {

    @Test
    void loadParams() {
        LiquibaseClickHouseConfig params = ParamsLoader.getLiquibaseClickhouseProperties("testLiquibaseClickhouse");
        assertInstanceOf(ClusterConfig.class, params);
        ClusterConfig clusterConfig = (ClusterConfig) params;
        assertEquals("Cluster1", clusterConfig.clusterName());
        assertEquals("Path1", clusterConfig.tableZooKeeperPathPrefix());
    }

    @Test
    void loadBrokenParams() {
        // In v2, underscore format (tableZooKeeperPath_Prefix) is now supported
        // and gets normalized to camelCase (tableZooKeeperPathPrefix)
        LiquibaseClickHouseConfig params =
            ParamsLoader.getLiquibaseClickhouseProperties("testLiquibaseClickhouseBroken");
        assertInstanceOf(ClusterConfig.class, params);
        ClusterConfig clusterConfig = (ClusterConfig) params;
        assertEquals("Cluster1", clusterConfig.clusterName());
        assertEquals("Path1", clusterConfig.tableZooKeeperPathPrefix());
    }
}
