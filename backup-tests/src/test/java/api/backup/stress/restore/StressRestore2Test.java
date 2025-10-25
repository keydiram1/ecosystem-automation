package api.backup.stress.restore;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressRestore2Test extends StressRestore {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns10";
        SOURCE_CLUSTER_NAME = "StressRestore2TestClusterName";
        BACKUP_NAMESPACE = "adr-ns10";
        BACKUP_NAME = "StressRestore2TestBackupName";
        POLICY_NAME = "StressRestore2TestPolicy";
        SET_NAME = "setStressRestore2Test";
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
