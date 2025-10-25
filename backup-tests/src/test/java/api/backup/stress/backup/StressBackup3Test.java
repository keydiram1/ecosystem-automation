package api.backup.stress.backup;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackup3Test extends StressBackup {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns3";
        SOURCE_CLUSTER_NAME = "StressBackup3TestClusterName";
        BACKUP_NAMESPACE = "adr-ns3";
        BACKUP_NAME = "StressBackup3TestBackupName";
        POLICY_NAME = "StressBackup3TestPolicy";
        SET_NAME = "setStressBackup3Test";
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
