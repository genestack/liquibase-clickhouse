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
package liquibase.ext.clickhouse.params;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import liquibase.Scope;
import liquibase.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Set;

public final class ParamsLoader {

    private ParamsLoader() {
    }

    private static final Logger LOG = Scope.getCurrentScope().getLog(ParamsLoader.class);

    private static final String CONF_FILE = "liquibaseClickhouse";

    private static final Set<String> VALID_PROPERTIES =
        new HashSet<>(Arrays.asList("clusterName", "tableZooKeeperPathPrefix"));

    private static StringBuilder appendWithComma(StringBuilder sb, String text) {
        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(text);

        return sb;
    }

    private static String getMissingProperties(Set<String> properties) {
        StringBuilder missingProperties = new StringBuilder();
        for (String validProperty: VALID_PROPERTIES) {
            if (!properties.contains(validProperty)) {
                appendWithComma(missingProperties, validProperty);
            }
        }

        return missingProperties.toString();
    }

    private static void checkProperties(Map<String, String> properties)
        throws InvalidPropertiesFormatException {
        StringBuilder errMsg = new StringBuilder();

        for (String key: properties.keySet()) {
            if (!VALID_PROPERTIES.contains(key)) {
                appendWithComma(errMsg, "unknown property: ").append(key);
            }
        }

        if (!errMsg.isEmpty() || properties.size() != VALID_PROPERTIES.size()) {
            appendWithComma(errMsg, "the missing properties should be defined: ");
            errMsg.append(getMissingProperties(properties.keySet()));
        }

        if (!errMsg.isEmpty()) {
            throw new InvalidPropertiesFormatException(errMsg.toString());
        }
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return sw.toString();
    }

    public static LiquibaseClickHouseConfig getLiquibaseClickhouseProperties() {
        try {
            String value = Scope.getCurrentScope().get("liquibase.clickhouse.configfile", String.class);
            if (value != null) {
                return getLiquibaseClickhouseProperties(value);
            }
        } catch (Exception ignored) {
        }
        return getLiquibaseClickhouseProperties(CONF_FILE);
    }

    public static LiquibaseClickHouseConfig getLiquibaseClickhouseProperties(String configFile) {
        Config conf = ConfigFactory.load(configFile);
        Map<String, String> params = new HashMap<>();
        LiquibaseClickHouseConfig result;

        try {
            conf.getConfig("cluster");
        } catch (ConfigException.Missing cem) {
            LOG.info(
                "Cluster settings ("
                    + configFile
                    + ".conf) are not defined. Work in single-instance clickhouse mode.");
            LOG.info(
                "The following properties should be defined: " + getMissingProperties(new HashSet<>()));
            return new StandaloneConfig();
        }

        for (Map.Entry<String, ConfigValue> s: conf.getConfig("cluster").entrySet()) {
            params.put(s.getKey(), s.getValue().unwrapped().toString());
        }

        try {
            checkProperties(params);
        } catch (InvalidPropertiesFormatException e) {
            LOG.severe(getStackTrace(e));
            LOG.severe("Fallback to single-instance clickhouse mode.");
            return new StandaloneConfig();
        }
        result = new ClusterConfig(params.get("clusterName"), params.get("tableZooKeeperPathPrefix"));
        LOG.info(
            "Cluster settings ("
                + configFile
                + ".conf) are found. Work in cluster replicated clickhouse mode.");

        return result;
    }
}
