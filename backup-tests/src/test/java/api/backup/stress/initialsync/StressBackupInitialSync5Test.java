package api.backup.stress.initialsync;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackupInitialSync5Test extends StressBackupInitialSync {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns8";
        SOURCE_CLUSTER_NAME = "StressBackupInitialSync5TestClusterName";
        BACKUP_NAMESPACE = "adr-ns8";
        BACKUP_NAME = "StressBackupInitialSync5TestBackupName";
        POLICY_NAME = "StressBackupInitialSync5TestPolicy";
        SET_NAME = "setStressBackupInitialSync5Test";
        setUpParent();
    }

    @AfterAll
    void afterAll() {
        afterAllParent();
    }

    @Test
    @Order(1)
    void createBackupWithInitialSync0() {
        createBackupWithInitialSync0Parent();
    }
}
