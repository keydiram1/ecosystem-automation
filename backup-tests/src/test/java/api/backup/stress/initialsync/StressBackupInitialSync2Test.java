package api.backup.stress.initialsync;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackupInitialSync2Test extends StressBackupInitialSync {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns5";
        SOURCE_CLUSTER_NAME = "StressBackupInitialSync2TestClusterName";
        BACKUP_NAMESPACE = "adr-ns5";
        BACKUP_NAME = "StressBackupInitialSync2TestBackupName";
        POLICY_NAME = "StressBackupInitialSync2TestPolicy";
        SET_NAME = "setStressBackupInitialSync2Test";
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
