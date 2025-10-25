package api.backup.stress.initialsync;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackupInitialSync4Test extends StressBackupInitialSync {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns7";
        SOURCE_CLUSTER_NAME = "StressBackupInitialSync4TestClusterName";
        BACKUP_NAMESPACE = "adr-ns7";
        BACKUP_NAME = "StressBackupInitialSync4TestBackupName";
        POLICY_NAME = "StressBackupInitialSync4TestPolicy";
        SET_NAME = "setStressBackupInitialSync4Test";
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
