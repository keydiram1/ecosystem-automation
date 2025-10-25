package api.backup.stress.restore;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressRestore4Test extends StressRestore {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns12";
        SOURCE_CLUSTER_NAME = "StressRestore4TestClusterName";
        BACKUP_NAMESPACE = "adr-ns12";
        BACKUP_NAME = "StressRestore4TestBackupName";
        POLICY_NAME = "StressRestore4TestPolicy";
        SET_NAME = "setStressRestore4Test";
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
