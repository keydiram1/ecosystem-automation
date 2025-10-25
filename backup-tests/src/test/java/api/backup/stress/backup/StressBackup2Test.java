package api.backup.stress.backup;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackup2Test extends StressBackup {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns2";
        SOURCE_CLUSTER_NAME = "StressBackup2TestClusterName";
        BACKUP_NAMESPACE = "adr-ns2";
        BACKUP_NAME = "StressBackup2TestBackupName";
        POLICY_NAME = "StressBackup2TestPolicy";
        SET_NAME = "setStressBackup2Test";
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
