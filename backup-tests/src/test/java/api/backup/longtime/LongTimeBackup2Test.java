package api.backup.longtime;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag("ADR-LONG-TIME-TEST")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S2699")
class LongTimeBackup2Test extends LongTimeBackup {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns2";
        BACKUP_NAMESPACE = "adr-ns2";
        SOURCE_CLUSTER_NAME = "SourceClusterLongTimeBackup2Test";
        BACKUP_NAME = "StressBackup2TestBackupName";
        POLICY_NAME = "StressBackup2TestPolicy";
        SET_NAME = "setStressBackup2Test";
        setUpParent();
    }

    @Test
    void onGoingDataCreation() {
        onGoingDataCreationParent();
    }

    @Test
    void printProgress() {
        printProgressParent();
    }
}
