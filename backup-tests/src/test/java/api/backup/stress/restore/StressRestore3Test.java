package api.backup.stress.restore;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressRestore3Test extends StressRestore {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns11";
        SOURCE_CLUSTER_NAME = "StressRestore3TestClusterName";
        BACKUP_NAMESPACE = "adr-ns11";
        BACKUP_NAME = "StressRestore3TestBackupName";
        POLICY_NAME = "StressRestore3TestPolicy";
        SET_NAME = "setStressRestore3Test";
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
