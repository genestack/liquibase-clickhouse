Forked from [liquibase-clickhouse](https://github.com/MEDIARITHMICS/liquibase-clickhouse) to enable support of clickhouse cluster.

# liquibase-clickhouse [![maven][maven-image]][maven-url]

[maven-image]: https://img.shields.io/maven-central/v/com.genestack/liquibase-clickhouse.svg?maxAge=259200&style=for-the-badge&color=brithgreen&label=com.genestack:liquibase-clickhouse
[maven-url]: https://mvnrepository.com/artifact/com.genestack/liquibase-clickhouse

The main difference from the original version is the ability to work with Clickhouse in a cluster mode
using zookeeper for storing information about migrations.

Maven dependency:

```
<dependency>
    <groupId>com.genestack</groupId>
    <artifactId>liquibase-clickhouse</artifactId>
    <version>Latest version</version>
</dependency>
```

The cluster mode can be activated by adding the **_liquibaseClickhouse.conf_** file
to the classpath (resource/).
```
cluster {
    clusterName="{cluster}"
    tableZooKeeperPathPrefix="/liquibase"
}
```
where `clusterName` is the name of the ClickHouse cluster where the migrations tables will be stored,
and `tableZooKeeperPathPrefix` is the prefix of the path in zookeeper where the migrations tables will
be stored.

To use this plugin in cluster mode, you need to enable the `KeeperMap` table engine
by adding the following line to the clickhouse-server configuration file:
```xml
<clickhouse>
  ...
  <keeper_map_path_prefix>
    /keeper/path/to/liquibase
  </keeper_map_path_prefix>
</clickhouse>
```

These example configuration will configure the plugin to create two
paths in your zookeeper:
- `/keeper/path/to/liquibase/liquibase/DATABASECHANGELOG`
- `/keeper/path/to/liquibase/liquibase/DATABASECHANGELOGLOCK`

The library also provides ClickHouseScopeUtils,
which allows configuring the ClickHouse configuration file per Liquibase execution.

```java
ClickHouseScopeUtils.withConfig(
        "cluster.conf",
        liquibase::afterPropertiesSet
);
```

The configuration file must be located in the application classpath
(typically in the resources directory). Only the file name should be specified.

This allows using multiple ClickHouse datasources in the same application,
where some datasources use cluster mode, others work in standalone mode,
or are connected to different ClickHouse clusters.
<hr/>

###### Important changes
 - 0.8.5:
   - Fixed SQL generation issues in specific cases:
     - when rerunning `runAlways` changesets
     - when updating checksums in case they differ from the ones in the database
     - when tagging changesets
   - Versions of libraries and images were updated to the latest ones
   - Improved test coverage for various scenarios, including the above-mentioned issues
 - 0.8.4:
   - Major update of libraries and images
   - Support of v2 ClickHouse jdbc driver (v1 for standalone mode)
   - Aligning license with the original project
 - Since version 0.8.1 the plugin uses Zookeeper for storing information about migrations.
 - The version 0.8.0 is not back-compatible with previous versions.
 - From the version 0.8.0 the extension adapted for the liquibase v4.26, Java baseline changed to 17, clickhouse baseline is
24.1.6. Changed synchronization to more adapted one for cluster environment.

 - From the version 0.7.0 the liquibase-clickhouse supports replication on a cluster. Liquibase v4.6.1.

 - From the version 0.6.0 the extension adapted for the liquibase v4.3.5.


### Uploading to maven central:

1) Make sure that token for upload sucessfully configured in `settings.xml` file:

  ```shell
  └──╼ $ cat ~/.m2/settings.xml                                                                                                                                             <aws:091468197733>
  <settings>
      <servers>
          <server>
            <id>central</id>
            <username>token-key</username>
            <password>token-secret-key</password>
          </server>
      </servers>
  </settings>
  ```

2) Import public and private gpg key locally (located in keybase)

  ```shell
  gpg --import genestack-secret.pgp
  gpg --import genestack-public.pgp
  ```

3) OPTIONAL! Make sure public key is published.

  ```shell
  gpg --list-keys
  gpg --keyserver keyserver.ubuntu.com --search-key FINGERPRINT_FROM_FIRST_COMMAND
  ```

4) Finally publish the package

  ```shell
    MAVEN_GPG_PASSPHRASE='passphrase' ./mvnw clean deploy
  ```
