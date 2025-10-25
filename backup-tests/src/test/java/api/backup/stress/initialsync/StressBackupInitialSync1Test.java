package api.backup.stress.initialsync;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackupInitialSync1Test extends StressBackupInitialSync {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns4";
        SOURCE_CLUSTER_NAME = "StressBackupInitialSync1TestClusterName";
        BACKUP_NAMESPACE = "adr-ns4";
        BACKUP_NAME = "StressBackupInitialSync1TestBackupName";
        POLICY_NAME = "StressBackupInitialSync1TestPolicy";
        SET_NAME = "setStressBackupInitialSync1Test";
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
