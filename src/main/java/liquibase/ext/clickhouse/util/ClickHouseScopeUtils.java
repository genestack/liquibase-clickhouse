package liquibase.ext.clickhouse.util;

import java.util.Map;
import liquibase.Scope;
import liquibase.exception.LiquibaseException;

/**
 * Utility methods for executing Liquibase operations with
 * a dedicated ClickHouse configuration file.
 *
 * <p>
 * The configuration file is propagated through Liquibase {@link Scope}
 * using the {@value #CONFIG_FILE_SCOPE_KEY} scope key.
 * </p>
 *
 * <p>
 * This allows different Liquibase executions within the same application
 * to use different ClickHouse configurations, including:
 * </p>
 *
 * <ul>
 *     <li>clustered and standalone ClickHouse instances</li>
 *     <li>different ClickHouse clusters</li>
 * </ul>
 *
 * <p>
 * The configuration file must be available in the application classpath
 * (typically in the {@code resources} directory).
 * </p>
 */
public class ClickHouseScopeUtils {

    public static final String CONFIG_FILE_SCOPE_KEY = "liquibase.clickhouse.configfile";

    private ClickHouseScopeUtils() {
    }

    public static void withConfig(String clickhouseConfigFile, Scope.ScopedRunner<?> runner) throws LiquibaseException {
        if (clickhouseConfigFile == null || clickhouseConfigFile.isBlank()) {
            try {
                runner.run();
                return;
            } catch (LiquibaseException e) {
                throw e;
            } catch (Exception e) {
                throw new LiquibaseException(e);
            }
        }
        try {
            Scope.child(
                    Map.of(CONFIG_FILE_SCOPE_KEY, clickhouseConfigFile),
                    runner
            );
        } catch (LiquibaseException e) {
            throw e;
        } catch (Exception e) {
            throw new LiquibaseException(e);
        }
    }
}
