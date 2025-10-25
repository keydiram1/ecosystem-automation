package api.backup.stress.restore;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressRestore5Test extends StressRestore {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns13";
        SOURCE_CLUSTER_NAME = "StressRestore5TestClusterName";
        BACKUP_NAMESPACE = "adr-ns13";
        BACKUP_NAME = "StressRestore5TestBackupName";
        POLICY_NAME = "StressRestore5TestPolicy";
        SET_NAME = "setStressRestore5Test";
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

    @Test
    @Order(2)
    void restoreSet() {
        restoreSetParent();
    }
}
