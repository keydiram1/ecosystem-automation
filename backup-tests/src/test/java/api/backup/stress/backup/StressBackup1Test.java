package api.backup.stress.backup;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackup1Test extends StressBackup {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns1";
        SOURCE_CLUSTER_NAME = "StressBackup1TestClusterName";
        BACKUP_NAMESPACE = "adr-ns1";
        BACKUP_NAME = "StressBackup1TestBackupName";
        POLICY_NAME = "StressBackup1TestPolicy";
        SET_NAME = "setStressBackup1Test";
        setUpParent();
    }

    @AfterAll
    void afterAll() {
        afterAllParent();
    }

    @Test
    @Order(1)
    void createBackup() {
        createBackupParent();
    }
}
