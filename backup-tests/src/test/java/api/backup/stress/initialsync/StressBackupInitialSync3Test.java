package api.backup.stress.initialsync;

import org.junit.jupiter.api.*;

@Tag("ADR-STRESS-TEST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressBackupInitialSync3Test extends StressBackupInitialSync {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns6";
        SOURCE_CLUSTER_NAME = "StressBackupInitialSync3TestClusterName";
        BACKUP_NAMESPACE = "adr-ns6";
        BACKUP_NAME = "StressBackupInitialSync3TestBackupName";
        POLICY_NAME = "StressBackupInitialSync3TestPolicy";
        SET_NAME = "setStressBackupInitialSync3Test";
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
