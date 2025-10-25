package api.backup.longtime;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag("ADR-LONG-TIME-TEST")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("java:S2699")
class LongTimeBackup1Test extends LongTimeBackup {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns1";
        BACKUP_NAMESPACE = "adr-ns1";
        SOURCE_CLUSTER_NAME = "SourceClusterLongTimeBackup1Test";
        BACKUP_NAME = "StressBackup1TestBackupName";
        POLICY_NAME = "StressBackup1TestPolicy";
        SET_NAME = "setStressBackup1Test";
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
