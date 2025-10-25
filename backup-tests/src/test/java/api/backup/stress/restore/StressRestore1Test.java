package api.backup.stress.restore;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressRestore1Test extends StressRestore {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns9";
        SOURCE_CLUSTER_NAME = "StressRestore1TestClusterName";
        BACKUP_NAMESPACE = "adr-ns9";
        BACKUP_NAME = "StressRestore1TestBackupName";
        POLICY_NAME = "StressRestore1TestPolicy";
        SET_NAME = "setStressRestore1Test";
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
