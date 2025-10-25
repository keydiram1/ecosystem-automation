package api.backup;

import com.aerospike.client.Key;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import utils.*;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ADR-E2E-STATIC")
class StaticConfigTest extends BackupRunner {
    private static final String SET1 = "staticTestSet1";
    private static final String SET2 = "staticTestSet2";
    private static final String SET3 = "staticTestSet3";
    private static final String SET4 = "staticTestSet4";
    private static final String SOURCE_NAMESPACE = "source-ns12";
    private static final String SOURCE_CLUSTER_NAME = "StaticConfigCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns12";
    private static final String BACKUP_NAME = "StaticConfigBackup";
    private static final String POLICY_NAME = "StaticConfigPolicy";
    private static final String DC_NAME = "StaticConfigDC";
    private static final Key key = new Key(SOURCE_NAMESPACE, SET1, "key");
    private static final Key key2 = new Key(SOURCE_NAMESPACE, SET2, "key");

    private static final List<String> aerospikeConfFiles = List.of(
            ConfigParametersHandler.getParameter("user.dir") + "../devops/install/backup/conf/source/aerospike.conf",
            ConfigParametersHandler.getParameter("user.dir") + "../devops/install/backup/conf/cluster/source2/aerospike.conf",
            ConfigParametersHandler.getParameter("user.dir") + "../devops/install/backup/conf/cluster/source3/aerospike.conf"
    );

    private static final List<String> aerospikeSourceNames =
            List.of("aerospike-source", "aerospike-source2", "aerospike-source3");

    private static void restartAllNodes() {
        aerospikeSourceNames.forEach(DockerManager::restartAerospikeContainer);
    }

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    private static List<String> getBackupSets() {
        Response backupSetsList = BackupApi.getBackupSets(BACKUP_NAME);
        return backupSetsList.body().as(new TypeRef<>() {
        });
    }

    @BeforeEach
    public void setUp() {
        aerospikeConfFiles.forEach(FileBackupRestore::createFileBackup);
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @AfterEach
    void tearDown() {
        aerospikeConfFiles.forEach(FileBackupRestore::restoreFileFromBackup);
        restartAllNodes();
    }

    // Removed set1 (from a single node) should still be shipped and processed by other nodes
    @Test
    void removeSet1From1Node() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(2), "ship-set " + SET1, "");
        DockerManager.restartAerospikeContainer(aerospikeSourceNames.get(2));
        assertThat(getBackupSets()).hasSameElementsAs(List.of(SET1, SET2, SET3));

        long afterLastRestart = System.currentTimeMillis();

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        AerospikeDataUtils.put(key2, "time", System.currentTimeMillis());
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        Awaitility.await("Wait for backups in set1 and set2")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key, key2));
    }

    // Removed set1 (from 2 nodes) should still be shipped and processed by the last node
    @Test
    void removeSet1From2Nodes() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(0), "ship-set " + SET1, "");
        DockerManager.restartAerospikeContainer(aerospikeSourceNames.get(0));
        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(2), "ship-set " + SET1, "");
        DockerManager.restartAerospikeContainer(aerospikeSourceNames.get(2));
        assertThat(getBackupSets()).hasSameElementsAs(List.of(SET1, SET2, SET3));

        long afterLastRestart = System.currentTimeMillis();

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        AerospikeDataUtils.put(key2, "time", System.currentTimeMillis());
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        Awaitility.await("Wait for backups in set1 and set2")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key, key2));
    }

    // Removed set1 shouldn't be shipped and processed
    @Test
    void removeSet1FromAllNodes() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        aerospikeConfFiles.forEach(it -> FileBackupRestore.replaceSubstringInFile(it, "ship-set " + SET1, ""));
        restartAllNodes();

        assertThat(getBackupSets()).hasSameElementsAs(List.of(SET2, SET3));
        long afterLastRestart = System.currentTimeMillis();

        Awaitility.await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        AerospikeDataUtils.put(key2, "time", System.currentTimeMillis());
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        Awaitility.await()
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> BackupManager.backupForKeyExist(BACKUP_NAME, key2));

        assertThat(BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key))
                .as("Should only have backup for set2")
                .isFalse();
    }

    // New set4 should be shipped and processed
    @Test
    void addSet4To1Node() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);
        assertThat(getBackupSets()).hasSameElementsAs(List.of(SET1, SET2, SET3));

        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(0),
                "ship-set %s".formatted(SET1),
                "ship-set %s\n            ship-set %s".formatted(SET1, SET4));

        restartAllNodes(); //for whatever reason we have to restart all nodes for test to pass

        assertThat(getBackupSets()).hasSameElementsAs(List.of(SET1, SET2, SET3, SET4));

        long afterLastRestart = System.currentTimeMillis();

        List<Key> keys4 = IntStream.range(0, 100)
                .mapToObj(i -> new Key(SOURCE_NAMESPACE, SET4, "key" + i + UUID.randomUUID()))
                .toList();

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        keys4.forEach(key -> AerospikeDataUtils.put(key, "time", System.currentTimeMillis()));
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        Awaitility.await("Wait for backup in set1")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> BackupManager.backupForKeyExist(BACKUP_NAME, key));

        boolean anyKeyBackedUp = keys4.stream().anyMatch(key -> BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key));
        assertThat(anyKeyBackedUp)
                .as("Should have some backups for set4")
                .isTrue();
    }

    // Removed namespace shouldn't be shipped and processed
    @Test
    void removeNamespaceFromAllNodes() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        aerospikeConfFiles.forEach(it -> FileBackupRestore.replaceSubstringInFile(it, """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-set staticTestSet2
                            ship-set staticTestSet3
                            ship-nsup-deletes true
                        }""", ""));
        restartAllNodes();

        long afterLastRestart = System.currentTimeMillis();

        Awaitility.await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        AerospikeDataUtils.put(key2, "time", System.currentTimeMillis());
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        AutoUtils.sleep(10_000);

        long to = System.currentTimeMillis();
        // We shouldn't be able to read backups due to no namespace mapping.
        // Originally its RestStatusException but its custom and considered a RuntimeException
        assertThatThrownBy(() -> MetadataAPI.readBackupTimestampsForKey(BACKUP_NAME, afterLastRestart, to, key))
            .isInstanceOf(RuntimeException.class).hasMessageContaining("No remote namespace found");
    }

    // Removed namespace (from a single node) should still be shipped and processed
    @Test
    void removeNamespaceFrom1Node() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(0), """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-set staticTestSet2
                            ship-set staticTestSet3
                            ship-nsup-deletes true
                        }""", "");
        DockerManager.restartAerospikeContainer(aerospikeSourceNames.get(0));

        long afterLastRestart = System.currentTimeMillis();

        List<Key> keys = IntStream.range(0, 20)
                .mapToObj(i -> new Key(SOURCE_NAMESPACE, SET1, "key" + i))
                .toList();

        Awaitility.await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    try {
                        keys.forEach(key -> AerospikeDataUtils.put(key, "time", System.currentTimeMillis()));
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        AutoUtils.sleep(15_000);

        // Only nodes that configured with XDR namespace will ship records changes, so we insert 20 records and
        // verify that some are still shipping and being processed by ADR
        boolean anyKeyBackedUp = keys.stream().anyMatch(key -> BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key));
        assertThat(anyKeyBackedUp)
                .as("Should have some backups")
                .isTrue();
    }

    // New namespace should be shipped and processed
    @Test
    void addNamespaceTo1Node() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        // Remove XDR config from all nodes
        aerospikeConfFiles.forEach(it -> FileBackupRestore.replaceSubstringInFile(it, """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-set staticTestSet2
                            ship-set staticTestSet3
                            ship-nsup-deletes true
                        }""", ""));
        restartAllNodes();

        // Add XDR config to a single node
        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(0), """
                namespace source-ns13 {
                            remote-namespace adr-ns13""", """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-set staticTestSet2
                            ship-set staticTestSet3
                            ship-nsup-deletes true
                        }
                        namespace source-ns13 {
                            remote-namespace adr-ns13""");
        restartAllNodes(); // for whatever reason we have to restart all nodes for test to pass

        long afterLastRestart = System.currentTimeMillis();
        List<Key> keys = IntStream.range(0, 20)
                .mapToObj(i -> new Key(SOURCE_NAMESPACE, SET1, "key" + i))
                .toList();

        Awaitility.await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    try {
                        keys.forEach(key -> AerospikeDataUtils.put(key, "time", System.currentTimeMillis()));
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        AutoUtils.sleep(15_000);

        // Only nodes that configured with XDR namespace will ship records changes, so we insert 20 records and
        // verify that some are still shipping and being processed by ADR
        boolean anyKeyBackedUp = keys.stream().anyMatch(key -> BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key));
        assertThat(anyKeyBackedUp)
                .as("Should have some backups")
                .isTrue();
    }

    // Remove ship-only-specified-sets from 1 node, meaning ship entire namespace
    // For that node and process all existing sets
    @Test
    void removeShippedOnlySpecifiedSetsFrom1Node() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(0), """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-set staticTestSet2
                            ship-set staticTestSet3
                            ship-nsup-deletes true
                        }""",
                """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets false
                            ship-nsup-deletes true
                        }""");
        restartAllNodes();

        long afterLastRestart = System.currentTimeMillis();
        List<Key> keys4 = IntStream.range(0, 100)
                .mapToObj(i -> new Key(SOURCE_NAMESPACE, SET4, "key" + i + UUID.randomUUID()))
                .toList();

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        keys4.forEach(key -> AerospikeDataUtils.put(key, "time", System.currentTimeMillis()));
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        AutoUtils.sleep(1_000);

        Awaitility.await("Wait for backup in set1")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> BackupManager.backupForKeyExist(BACKUP_NAME, key));

        boolean anyKeyBackedUp = keys4.stream().anyMatch(key -> BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key));
        assertThat(anyKeyBackedUp)
                .as("Should have backup for both set1 and set4")
                .isTrue();
    }

    // Add ship-only-specified-sets to all nodes, meaning ship only specific sets, not entire namespace
    // Unspecified set should not be shipped and processed
    @Test
    void addShippedOnlySpecifiedSetsToAllNodes() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, 86400, DC_NAME);

        aerospikeConfFiles.forEach(it -> {
            FileBackupRestore.replaceSubstringInFile(it, """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-set staticTestSet2
                            ship-set staticTestSet3
                            ship-nsup-deletes true
                        }""",
                    """
                    namespace source-ns12 {
                                remote-namespace adr-ns12
                                max-throughput 0
                                ship-only-specified-sets false
                                ship-nsup-deletes true
                            }""");

        });
        restartAllNodes();

        assertThat(getBackupSets()).isEmpty();

        //bring back set1 for 1 node
        FileBackupRestore.replaceSubstringInFile(aerospikeConfFiles.get(0), """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets false
                            ship-nsup-deletes true
                        }"""
                ,
                """
                namespace source-ns12 {
                            remote-namespace adr-ns12
                            max-throughput 0
                            ship-only-specified-sets true
                            ship-set staticTestSet1
                            ship-nsup-deletes true
                        }"""
        );
        restartAllNodes();
        assertThat(getBackupSets()).hasSameElementsAs(List.of(SET1));

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                        return true;
                    } catch (Exception error) {
                        AerospikeLogger.info(error);
                        return false;
                    }
                });

        BackupManager.waitForBackup(BACKUP_NAME, key, 1, 20);
    }

    // Static XDR config should be persisted and tolerate node restarts
    @Test
    void noBackupAfterRestart() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                List.of(SET1), 86400, DC_NAME);

        ScheduledFuture<?> scheduledFuture = newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> {
                    try {
                        AerospikeDataUtils.put(key, "time", System.currentTimeMillis());
                    } catch (Exception e) {
                        AerospikeLogger.info("Could not put data");
                    }
                }, 0, 1_000, TimeUnit.MILLISECONDS
        );

        AutoUtils.sleep(10_000);
        assertThat(BackupManager.backupForKeyExist(BACKUP_NAME, key))
                .as(() -> {
                    scheduledFuture.cancel(true);
                    return "Should have at least one backup created by now";
                })
                .isTrue();

        restartAllNodes();

        long afterLastRestart = System.currentTimeMillis();
        AutoUtils.sleep(10_000);

        scheduledFuture.cancel(true);
        assertThat(BackupManager.backupForKeyExist(BACKUP_NAME, afterLastRestart, key))
                .as("backup after nodes restart should exist")
                .isTrue();
    }
}
